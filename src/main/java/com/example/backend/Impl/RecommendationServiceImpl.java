package com.example.backend.Impl;

import com.example.backend.Dao.ProductMapper;
import com.example.backend.Dao.RecommendationMapper;
import com.example.backend.Entity.FlashSale;
import com.example.backend.Entity.Product;
import com.example.backend.Entity.Recommendation;
import com.example.backend.Entity.ProductResponse;
import com.example.backend.Service.RecommendationService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private RecommendationMapper recommendationMapper;

    @Override
    public List<ProductResponse> recommendProducts(int topN) throws IOException {
        List<Product> allProducts = productMapper.getAllProducts();
        if (allProducts.isEmpty()) {
            logger.info("No products found in the database.");
            return new ArrayList<>();
        }
        System.out.println("allProducts: " + allProducts);

        // 随机选择一个产品作为目标产品
        Product targetProduct = pickRandomProduct(allProducts);
        Long targetProductId = targetProduct.getProduct_id();

        Directory index = new RAMDirectory();
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter writer = new IndexWriter(index, config);
        for (Product product : allProducts) {
            Document doc = new Document();
            doc.add(new TextField("product_id", String.valueOf(product.getProduct_id()), Field.Store.YES));
            doc.add(new TextField("description", product.getDescription(), Field.Store.YES));
            doc.add(new TextField("product_name", product.getProduct_name(), Field.Store.YES));
            writer.addDocument(doc);
        }
        writer.close();

        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);

        if (targetProduct == null) {
            logger.info("Target product with id {} not found in the database.", targetProductId);
            return new ArrayList<>();
        }

        QueryParser parser = new QueryParser("description", analyzer);
        Query query = null;
        try {
            query = parser.parse(targetProduct.getDescription());
            logger.info("Generated query from target product description: {}", targetProduct.getDescription());
        } catch (ParseException e) {
            logger.error("Failed to parse query from target product description: {}", targetProduct.getDescription(), e);
            throw new RuntimeException(e);
        }

        TopDocs topDocs = searcher.search(query, topN + 1);
        logger.info("Search returned {} hits for target product with id {}.", topDocs.totalHits, targetProductId);
        List<Product> recommendedProductsByDescription = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String idStr = doc.get("product_id");
            Long id = parseId(idStr);
            if (id == null || id.equals(targetProductId)) {
                continue;
            }
            Product product = productMapper.getProductById(id);
            if (product!= null) {
                recommendedProductsByDescription.add(product);
            } else {
                logger.warn("Product with id {} not found in the database.", id);
            }
        }

        // 基于商品名称的推荐
        QueryParser nameParser = new QueryParser("product_name", analyzer);
        Query nameQuery = null;
        try {
            nameQuery = nameParser.parse(targetProduct.getProduct_name());
            logger.info("Generated query from target product name: {}", targetProduct.getProduct_name());
        } catch (ParseException e) {
            logger.error("Failed to parse query from target product name: {}", targetProduct.getProduct_name(), e);
            throw new RuntimeException(e);
        }

        TopDocs nameTopDocs = searcher.search(nameQuery, topN + 1);
        logger.info("Search returned {} hits for target product name with id {}.", nameTopDocs.totalHits, targetProductId);
        List<Product> recommendedProductsByName = new ArrayList<>();
        for (ScoreDoc scoreDoc : nameTopDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String idStr = doc.get("product_id");
            Long id = parseId(idStr);
            if (id == null || id.equals(targetProductId)) {
                continue;
            }
            Product product = productMapper.getProductById(id);
            if (product!= null) {
                recommendedProductsByName.add(product);
            } else {
                logger.warn("Product with id {} not found in the database.", id);
            }
        }

        reader.close();

        // 额外步骤：根据推荐表中的推荐信息调整推荐结果
        List<Recommendation> recommendations = recommendationMapper.getRecommendationsByProductId(targetProductId);
        logger.info("Found {} recommendations from the recommendation table for target product with id {}.", recommendations.size(), targetProductId);
        List<Product> finalRecommendations = new ArrayList<>();
        for (Recommendation recommendation : recommendations) {
            Product product = productMapper.getProductById(recommendation.getProductId());
            if (product!= null) {
                finalRecommendations.add(product);
            } else {
                logger.warn("Product with id {} not found in the database, referenced in recommendation table.", recommendation.getProductId());
            }
        }

        // 合并三种推荐结果
        recommendedProductsByDescription.addAll(recommendedProductsByName);
        recommendedProductsByDescription.addAll(finalRecommendations);

        // 去除重复的产品
        List<Product> distinctRecommendations = recommendedProductsByDescription.stream()
                .distinct()
                .collect(Collectors.toList());

        // 随机打乱推荐列表
        Collections.shuffle(distinctRecommendations);

        int length = distinctRecommendations.size();
        if (length >= topN) {
            // 取前 topN 个元素
            List<Product> topNRecommendations = distinctRecommendations.stream()
                    .limit(topN)
                    .collect(Collectors.toList());

            // 转换为 ProductResponse 列表
            List<ProductResponse> topNRecommendationsInfo = topNRecommendations.stream()
                    .map(product -> {
                        FlashSale flashSale = productMapper.getFlashSaleByProductId(product.getProduct_id());
                        return new ProductResponse(product, flashSale);
                    })
                    .collect(Collectors.toList());

            System.out.println("Top " + topN + " recommended products: " + topNRecommendationsInfo);

            return topNRecommendationsInfo;
        } else if (length < topN) {
            // 当推荐结果数量不足 topN 时，从 allProducts 中随机选取产品添加到推荐列表
            while (distinctRecommendations.size() < topN) {
                Product randomProduct = pickRandomProduct(allProducts);
                if (!distinctRecommendations.contains(randomProduct)) {
                    distinctRecommendations.add(randomProduct);
                }
            }

            // 转换为 ProductResponse 列表
            List<ProductResponse> topNRecommendationsInfo = distinctRecommendations.stream()
                    .map(product -> {
                        FlashSale flashSale = productMapper.getFlashSaleByProductId(product.getProduct_id());
                        return new ProductResponse(product, flashSale);
                    })
                    .collect(Collectors.toList());

            System.out.println("Top " + topN + " recommended products: " + topNRecommendationsInfo);

            return topNRecommendationsInfo;
        }

        return new ArrayList<>();
    }

    // 辅助方法：将字符串解析为 Long 类型，如果字符串无效则返回 null
    private Long parseId(String idStr) {
        if (idStr == null || idStr.isEmpty() || "null".equalsIgnoreCase(idStr)) {
            return null;
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid id string: {}", idStr);
            return null;
        }
    }

    // 辅助方法：从产品列表中随机选择一个产品
    private Product pickRandomProduct(List<Product> allProducts) {
        if (allProducts.isEmpty()) {
            return null;
        }
        Random random = new Random();
        int randomIndex = random.nextInt(allProducts.size());
        return allProducts.get(randomIndex);
    }
}