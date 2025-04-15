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
import java.util.*;
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
        System.out.println(promotions);
        // 遍历商品促销列表，根据促销类型计算折扣价格
        for (ProductPromotion promotion : promotions) {
            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
            // 设置计算得到的折扣价格
            promotion.setDiscount_price(discountPrice);
        }
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
                promotions
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
        System.out.println(productPromotions);
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

                    ProductPromotion cheapestPromotion = null;
                    BigDecimal lowestDiscountPrice = null;
                    List<ProductPromotion> flashSale = productMapper.getFlashSaleByProductId(productId);
                    for (ProductPromotion promotion : flashSale) {
                        if (promotion != null) {
                            System.out.println("Flash sale found for product ID " + promotion);
                            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
                            // 将计算得到的折扣价格设置到 ProductPromotion 对象中
                            promotion.setDiscount_price(discountPrice);

                            if (lowestDiscountPrice == null || discountPrice.compareTo(lowestDiscountPrice) < 0) {
                                lowestDiscountPrice = discountPrice;
                                cheapestPromotion = promotion;
                            }
                        }
                    }
                    // 创建 ProductResponse 对象并设置相关信息
                    return new ProductResponse(product, cheapestPromotion);
                }).collect(Collectors.toList());
                ProductResponsePageResult productResponsePageResult = new ProductResponsePageResult(responseList,total);
                return ResponseEntity.ok().body(productResponsePageResult);
            }
        } catch (Exception e) {
            System.out.println("Error occurred while fetching apple phone products: " + e);
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

                    ProductPromotion cheapestPromotion = null;
                    BigDecimal lowestDiscountPrice = null;
                    List<ProductPromotion> flashSale = productMapper.getFlashSaleByProductId(productId);
                    for (ProductPromotion promotion : flashSale) {
                        if (promotion != null) {
                            System.out.println("Flash sale found for product ID " + promotion);
                            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
                            // 将计算得到的折扣价格设置到 ProductPromotion 对象中
                            promotion.setDiscount_price(discountPrice);

                            if (lowestDiscountPrice == null || discountPrice.compareTo(lowestDiscountPrice) < 0) {
                                lowestDiscountPrice = discountPrice;
                                cheapestPromotion = promotion;
                            }
                        }
                    }

                    return new ProductResponse(product, cheapestPromotion);
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

                    ProductPromotion cheapestPromotion = null;
                    BigDecimal lowestDiscountPrice = null;
                    List<ProductPromotion> flashSale = productMapper.getFlashSaleByProductId(productId);
                    for (ProductPromotion promotion : flashSale) {
                        if (promotion != null) {
                            System.out.println("Flash sale found for product ID " + promotion);
                            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
                            // 将计算得到的折扣价格设置到 ProductPromotion 对象中
                            promotion.setDiscount_price(discountPrice);

                            if (lowestDiscountPrice == null || discountPrice.compareTo(lowestDiscountPrice) < 0) {
                                lowestDiscountPrice = discountPrice;
                                cheapestPromotion = promotion;
                            }
                        }
                    }

                    return new ProductResponse(product, cheapestPromotion);
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

                    ProductPromotion cheapestPromotion = null;
                    BigDecimal lowestDiscountPrice = null;
                    List<ProductPromotion> flashSale = productMapper.getFlashSaleByProductId(productId);
                    for (ProductPromotion promotion : flashSale) {
                        if (promotion != null) {
                            System.out.println("Flash sale found for product ID " + promotion);
                            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
                            // 将计算得到的折扣价格设置到 ProductPromotion 对象中
                            promotion.setDiscount_price(discountPrice);

                            if (lowestDiscountPrice == null || discountPrice.compareTo(lowestDiscountPrice) < 0) {
                                lowestDiscountPrice = discountPrice;
                                cheapestPromotion = promotion;
                            }
                        }
                    }
                   
                    return new ProductResponse(product, cheapestPromotion);
                }).collect(Collectors.toList());
                ProductResponsePageResult productResponsePageResult = new ProductResponsePageResult(responseList,total);
                return ResponseEntity.ok().body(productResponsePageResult);
            }
        } catch (Exception e){
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    @Override
    public List<ProductPayInfo> batchGetProductDetails(List<Long> productIds, Integer userId) {
        List<ProductPayInfo> result = new ArrayList<>();
        for (Long productId : productIds) {
            ProductPayInfo productDetails = productMapper.getProductDetails(productId);
            System.out.println(productDetails);
            if (productDetails != null) {
                // 查询用户之前使用过的促销 ID
                List<Integer> usedPromotions = productMapper.getUserUsedPromotions(userId, productId);
                // 查询当前可用的促销信息
                List<ProductPromotion> availablePromotions = productMapper.getAvailablePromotions(userId, productId);
                // 移除用户已经使用过的促销信息
                availablePromotions.removeIf(promotion -> usedPromotions.contains(promotion.getPromotion_id()));

                ProductImage main_img = productImageMapper.getProductMainImageByProductId(productId);
                productDetails.setProduct_main_image(main_img.getImage_url());
                // 筛选出最优惠的促销活动
                ProductPromotion bestPromotion = findBestPromotion(availablePromotions,productDetails.getPrice());
                System.out.println(bestPromotion);
                if (bestPromotion != null) {
                    productDetails.setPromotions(List.of(bestPromotion));
                } else {
                    productDetails.setPromotions(List.of());
                }
                result.add(productDetails);
            }
        }
        return result;
    }



    private ProductPromotion findBestPromotion(List<ProductPromotion> promotions, BigDecimal price) {
        if (promotions.isEmpty()) {
            return null;
        }
        return promotions.stream()
                .max(Comparator.comparing(promotion -> {
                    if ("DISCOUNT".equals(promotion.getPromotion_type())) {
                        // 计算折扣类型的优惠金额
                        return price.multiply(BigDecimal.ONE.subtract(promotion.getDiscount_rate()));
                    } else if ("REDUCE_AMOUNT".equals(promotion.getPromotion_type())) {
                        // 满减类型的优惠金额就是满减的数值
                        return promotion.getReduce_amount();
                    }
                    return BigDecimal.ZERO;
                }))
                .orElse(null);
    }
}
