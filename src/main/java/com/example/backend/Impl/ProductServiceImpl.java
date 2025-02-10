package com.example.backend.Impl;

import com.example.backend.Dao.*;
import com.example.backend.Entity.*;
import com.example.backend.Service.ProductService;
import com.example.backend.Utils.PromotionDiscountCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private ProductImageMapper productImageMapper;
    @Autowired
    private ProductPromotionMapper productPromotionMapper;
    @Autowired
    private ProductReviewMapper productReviewMapper;

    @Override
    public ResponseEntity<ProductDetails> getProductDetails(Long productId) {
        Product product = productMapper.getProductById(productId);
        if (product == null) {
            throw new RuntimeException("Product not found with id: " + productId);
        }
        Category category = categoryMapper.getCategoryById(product.getCategory_id());
        Brand brand = brandMapper.getBrandById(product.getBrand_id());
        if (category == null || brand == null) {
            throw new RuntimeException("Category or Brand not found for product with id: " + productId);
        }

        List<ProductImage> productImages = productImageMapper.getProductImagesByProductId(productId);
        List<String> imageUrls = new ArrayList<>();
        for (ProductImage image : productImages) {
            imageUrls.add(image.getImage_url());
        }

        List<ProductPromotion> promotions = productPromotionMapper.getProductPromotionsByProductId(productId);
        // 遍历商品促销列表，根据促销类型计算折扣价格
        for (ProductPromotion promotion : promotions) {
            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
            // 设置计算得到的折扣价格
            promotion.setDiscount_price(discountPrice);
        }
        List<ProductReview> reviews = productReviewMapper.getReviewsByProductId(productId);
        return ResponseEntity.ok().body(new ProductDetails(
                product.getProduct_id(),
                product.getProduct_name(),
                category.getCategory_name(),
                brand.getBrand_name(),
                product.getProduct_description(),
                product.getPrice(),
                product.getQuality(),
                product.getStock(),
                imageUrls,
                promotions,
                reviews
        ));
    }

    @Override
    public ResponseEntity<List<Map<String, Object>>> selectNewList(int num) {
        try {
            List<Product> newList = productMapper.getNewList(num);
            if (newList != null) {
                List<Map<String, Object>> infoList = new ArrayList<>();
                for (Product product : newList) {
                    Long productId = product.getProduct_id();
                    ProductImage mainImage = productImageMapper.getProductMainImageByProductId(productId);
                    product.setProduct_main_image(mainImage.getImage_url());
                    Map<String, Object> info = new HashMap<>();
                    info.put("product_id", productId);
                    info.put("product_name", product.getProduct_name());
                    info.put("product_main_image", product.getProduct_main_image());
                    info.put("price", product.getPrice());
                    info.put("quality", product.getQuality());
                    infoList.add(info);
                }
                return ResponseEntity.ok().body(infoList);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public ResponseEntity<List<ProductPromotion>> selectFlashSalesList(int num) {
        // 调用 Mapper 方法查询限时活动商品列表
        List<ProductPromotion> productPromotions = productPromotionMapper.selectFlashSalesList(num);
        // 遍历商品促销列表，根据促销类型计算折扣价格
        for (ProductPromotion promotion : productPromotions) {
            Long productId = promotion.getProduct_id();
            ProductImage mainImage = productImageMapper.getProductMainImageByProductId(productId);
            promotion.setProduct_main_image(mainImage.getImage_url());
            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
            // 设置计算得到的折扣价格
            promotion.setDiscount_price(discountPrice);
        }

        // 返回包含结果列表的响应实体，状态码为 200 OK
        return new ResponseEntity<>(productPromotions, HttpStatus.OK);
    }

    public ResponseEntity<ProductResponsePageResult> selectApplePhoneProductList(int page, int size, String sortField, String sortOrder) {
        try {
            int offset = (page - 1) * size;
            String categoryName = "手机";
            String brandName = "苹果（Apple）";
            List<Product> applePhoneProductList = productMapper.getProductList(categoryName, brandName, offset, size, sortField, sortOrder);
            int total = productMapper.getProductTotal(categoryName, brandName);
            if (applePhoneProductList != null) {
                List<ProductResponse> responseList = applePhoneProductList.stream().map(product -> {
                    // 获取产品ID
                    Long productId = product.getProduct_id();
                    if (productId == null) {
                        // 处理产品ID为空的情况
                        return null;
                    }
                    ProductImage mainImage = productImageMapper.getProductMainImageByProductId(productId);
                    product.setProduct_main_image(mainImage.getImage_url());
                    ProductPromotion flashSale = productMapper.getFlashSaleByProductId(productId);
                    if (flashSale != null) {
                        BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(flashSale);
                        // 将计算得到的折扣价格设置到 ProductPromotion 对象中
                        flashSale.setDiscount_price(discountPrice);
                    }
                    // 创建 ProductResponse 对象并设置相关信息
                    return new ProductResponse(product, flashSale);
                }).collect(Collectors.toList());
                ProductResponsePageResult productResponsePageResult = new ProductResponsePageResult(responseList,total);
                return ResponseEntity.ok().body(productResponsePageResult);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }
    @Override
    public ResponseEntity<ProductResponsePageResult> selectOrderPhoneProductList(int page, int size, String sortField, String sortOrder) {
        try {
            int offset = (page - 1) * size;
            String categoryName = "手机";
            List<Product> OtherPhoneProductList = productMapper.getOtherPhoneProductList(categoryName, offset, size, sortField, sortOrder);
            int total = productMapper.getOtherPhoneProductTotal(categoryName);
            if (OtherPhoneProductList != null) {
                List<ProductResponse> responseList = OtherPhoneProductList.stream().map(product -> {
                    // 获取产品ID
                    Long productId = product.getProduct_id();
                    if (productId == null) {
                        // 处理产品ID为空的情况
                        return null; // 或者抛出异常，具体取决于您的需求
                    }
                    ProductImage mainImage = productImageMapper.getProductMainImageByProductId(productId);
                    product.setProduct_main_image(mainImage.getImage_url());
                    ProductPromotion flashSale = productMapper.getFlashSaleByProductId(productId);
                    if (flashSale != null) {
                        BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(flashSale);
                        // 将计算得到的折扣价格设置到 ProductPromotion 对象中
                        flashSale.setDiscount_price(discountPrice);
                    }
                    return new ProductResponse(product, flashSale);
                }).collect(Collectors.toList());
                ProductResponsePageResult productResponsePageResult = new ProductResponsePageResult(responseList,total);
                return ResponseEntity.ok().body(productResponsePageResult);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    public ResponseEntity<ProductResponsePageResult> selectCategoryProductList(List<String> categoryName, int page, int size, String sortField, String sortOrder){
        try {
            int offset = (page - 1) * size;
            List<Product> OtherPhoneProductList = productMapper.getProductListByCategoryName(categoryName, offset, size, sortField, sortOrder);
            int total = productMapper.getProductTotalByCategoryName(categoryName);
            if (OtherPhoneProductList != null) {
                List<ProductResponse> responseList = OtherPhoneProductList.stream().map(product -> {
                    // 获取产品ID
                    Long productId = product.getProduct_id();
                    if (productId == null) {
                        // 处理产品ID为空的情况
                        return null; // 或者抛出异常，具体取决于您的需求
                    }
                    ProductImage mainImage = productImageMapper.getProductMainImageByProductId(productId);
                    product.setProduct_main_image(mainImage.getImage_url());
                    ProductPromotion flashSale = productMapper.getFlashSaleByProductId(productId);
                    if (flashSale != null) {
                        BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(flashSale);
                        // 将计算得到的折扣价格设置到 ProductPromotion 对象中
                        flashSale.setDiscount_price(discountPrice);
                    }
                    return new ProductResponse(product, flashSale);
                }).collect(Collectors.toList());
                ProductResponsePageResult productResponsePageResult = new ProductResponsePageResult(responseList,total);
                return ResponseEntity.ok().body(productResponsePageResult);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    public ResponseEntity<ProductResponsePageResult> SearchProductList(String selectedCategory, String selectedBrand, String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize){
        try {
            System.out.println(searchKeyword);
            Map<String, Object> params = new HashMap<>();
            params.put("selectedCategory", selectedCategory);
            params.put("selectedBrand", selectedBrand);
            params.put("searchKeyword", searchKeyword);
            params.put("sortField", sortField);
            params.put("sortOrder", sortOrder);
            params.put("offset", (currentPage - 1) * pageSize);
            params.put("pageSize", pageSize);
            List<Product> productList = productMapper.SearchProductList(params);
            Map<String, Object> countParams = new HashMap<>();
            countParams.put("selectedCategory", selectedCategory);
            countParams.put("selectedBrand", selectedBrand);
            countParams.put("searchKeyword", searchKeyword);
            int total = productMapper.getSearchProductTotal(countParams);
            if( productList != null){
                List<ProductResponse> responseList = productList.stream().map(product -> {
                    // 获取产品ID
                    Long productId = product.getProduct_id();
                    if (productId == null) {
                        // 处理产品ID为空的情况
                        return null; // 或者抛出异常，具体取决于您的需求
                    }
                    ProductImage mainImage = productImageMapper.getProductMainImageByProductId(productId);
                    product.setProduct_main_image(mainImage.getImage_url());
                    ProductPromotion flashSale = productMapper.getFlashSaleByProductId(productId);
                    if (flashSale != null) {
                        BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(flashSale);
                        // 将计算得到的折扣价格设置到 ProductPromotion 对象中
                        flashSale.setDiscount_price(discountPrice);
                    }
                    return new ProductResponse(product, flashSale);
                }).collect(Collectors.toList());
                ProductResponsePageResult productResponsePageResult = new ProductResponsePageResult(responseList,total);
                return ResponseEntity.ok().body(productResponsePageResult);
            }
        } catch (Exception e){
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }
}
