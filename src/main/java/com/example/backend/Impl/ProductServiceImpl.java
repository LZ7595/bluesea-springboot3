package com.example.backend.Impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.backend.Controller.ProductController;
import com.example.backend.Dao.*;
import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Dto.ProductPromotionWrapper;
import com.example.backend.Model.Dto.PromotionUsedCountDTO;
import com.example.backend.Model.Entity.*;
import com.example.backend.Model.Response.ProductResponse;
import com.example.backend.Model.Vo.ProductDetails;
import com.example.backend.Model.Vo.ProductPayInfo;
import com.example.backend.Service.ProductService;
import com.example.backend.Utils.PromotionDiscountCalculator;
import com.example.backend.Utils.PromotionUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.backend.Utils.RedisConstants.*;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;
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
    public ResponseEntity<ProductDetails> getProductDetails(Long productId, Integer userId) {
        log.info("getProductDetails: productId={}, userId={}", productId, userId);
        String redisKey = PRODUCT_DETAILS_KEY + productId;
        // 区分登录/未登录用户的缓存键
        String cacheKey = userId != null
                ? PRODUCT_PROMOTIONS_KEY + productId + ":user:" + userId
                : PRODUCT_PROMOTIONS_KEY + productId + ":anonymous";

        // 1. 尝试从Redis获取缓存
        String cachedDetails = stringRedisTemplate.opsForValue().get(redisKey);
        String cachedPromotions = stringRedisTemplate.opsForValue().get(cacheKey);

        // 1.1 商品基础信息和促销信息都命中缓存
        if (StrUtil.isNotBlank(cachedDetails) && StrUtil.isNotBlank(cachedPromotions)) {
            ProductDetails productDetails = JSONUtil.toBean(cachedDetails, ProductDetails.class);
            ProductPromotionWrapper promotionWrapper = JSONUtil.toBean(cachedPromotions, ProductPromotionWrapper.class);
            productDetails.setPromotionWrapper(promotionWrapper);
            return ResponseEntity.ok(productDetails);
        }

        // 2. 处理商品基础信息
        ProductDetails productDetails;
        if (StrUtil.isNotBlank(cachedDetails)) {
            // 2.1 商品基础信息命中缓存，直接反序列化
            productDetails = JSONUtil.toBean(cachedDetails, ProductDetails.class);
        } else {
            // 2.2 商品基础信息未命中缓存，从数据库查询
            Product product = productMapper.getProductById(productId);
            if (product == null) {
                stringRedisTemplate.opsForValue().set(redisKey, "{}", 5, TimeUnit.MINUTES);
                throw new RuntimeException("Product not found with id: " + productId);
            }

            Category category = categoryMapper.getCategoryById(product.getCategory_id());
            Brand brand = brandMapper.getBrandById(product.getBrand_id());
            if (category == null || brand == null) {
                throw new RuntimeException("Category or Brand not found for product with id: " + productId);
            }

            List<ProductImage> productImages = productImageMapper.getProductImagesByProductId(productId);
            List<String> imageUrls = productImages.stream()
                    .map(ProductImage::getImage_url)
                    .collect(Collectors.toList());

            // 构建基础商品详情（暂不包含促销信息）
            productDetails = new ProductDetails(
                    product.getProduct_id(),
                    product.getProduct_name(),
                    category.getCategory_name(),
                    brand.getBrand_name(),
                    product.getProduct_description(),
                    product.getPrice(),
                    product.getPrice(), // 临时设置，后续会更新
                    product.getQuality(),
                    product.getStock(),
                    imageUrls,
                    null // 促销信息后续单独处理
            );

            // 存入商品基础信息缓存
            stringRedisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(productDetails), 30, TimeUnit.MINUTES);
        }

        // 3. 处理促销信息（单独判断缓存是否命中）
        ProductPromotionWrapper promotionWrapper;
        if (StrUtil.isNotBlank(cachedPromotions)) {
            // 3.1 促销信息命中缓存，直接反序列化
            promotionWrapper = JSONUtil.toBean(cachedPromotions, ProductPromotionWrapper.class);
        } else {
            // 3.2 促销信息未命中缓存，从数据库查询
            promotionWrapper = new ProductPromotionWrapper();
            Product product = productMapper.getProductById(productId); // 确保获取最新商品信息（库存等）

            if (userId == null) {
                // 未登录用户
                List<ProductPromotion> allPromotions = productMapper.getAvailablePromotionsForAnonymous(productId);
                promotionWrapper.setUsablePromotions(filterAnonymousUsablePromotions(allPromotions, product.getStock()));
                promotionWrapper.setUnusablePromotions(Collections.emptyList());
            } else {
                // 已登录用户
                List<PromotionUsedCountDTO> usedCountList = productMapper.getPromotionUsedCountByUser(
                        userId,
                        Collections.singletonList(productId)
                );
                Map<Long, Integer> usedCountMap = new HashMap<>();
                if (usedCountList != null && !usedCountList.isEmpty()) {
                    for (PromotionUsedCountDTO dto : usedCountList) {
                        if (dto.getPromotionId() != null) {
                            usedCountMap.put(dto.getPromotionId(), dto.getUsedCount());
                        }
                    }
                }

                List<ProductPromotion> allPromotions = productMapper.getAvailablePromotions(userId, productId);
                PromotionUtil.splitUsableAndUnusablePromotions(
                        allPromotions, usedCountMap,
                        product.getStock(), promotionWrapper
                );
            }

            // 存入促销信息缓存
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(promotionWrapper), 10, TimeUnit.MINUTES);
        }

        // 4. 计算最佳价格并完善商品详情
        BigDecimal bestPrice = promotionWrapper.getUsablePromotions().stream()
                .map(ProductPromotion::getDiscount_price)
                .min(BigDecimal::compareTo)
                .orElse(productDetails.getPrice()); // 使用基础信息中的原价
        productDetails.setBestPrice(bestPrice);
        productDetails.setPromotionWrapper(promotionWrapper);

        return ResponseEntity.ok(productDetails);
    }

    /**
     * 过滤未登录用户的可用促销（仅判断活动库存和商品库存）
     */
    private List<ProductPromotion> filterAnonymousUsablePromotions(List<ProductPromotion> promotions, int productStock) {
        return promotions.stream()
                .filter(p -> p.getPromotion_quantity() > 0 && productStock > 0)
                .peek(p -> {
                    BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(p);
                    p.setDiscount_price(discountPrice);
                })
                .collect(Collectors.toList());
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

    public ResponseEntity<?> selectApplePhoneProductList(int page, int size, String sortField, String sortOrder) {
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
                PageResult<ProductResponse> PageResult = new PageResult<>(responseList, total, page, size, (int) Math.ceil((double) total / size));
                return ResponseEntity.ok().body(PageResult);
            }
        } catch (Exception e) {
            System.out.println("Error occurred while fetching apple phone products: " + e);
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    @Override
    public ResponseEntity<?> selectOrderPhoneProductList(int page, int size, String sortField, String sortOrder) {
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
                PageResult<ProductResponse> PageResult = new PageResult<>(responseList, total, page, size, (int) Math.ceil((double) total / size));
                return ResponseEntity.ok().body(PageResult);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    public ResponseEntity<?> selectCategoryProductList(List<String> categoryName, int page, int size, String sortField, String sortOrder) {
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
                PageResult<ProductResponse> PageResult = new PageResult<>(responseList, total, page, size, (int) Math.ceil((double) total / size));
                return ResponseEntity.ok().body(PageResult);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    public ResponseEntity<?> SearchProductList(Integer selectedCategory, Integer selectedBrand, String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize) {
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
            if (productList != null) {
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
                PageResult<ProductResponse> PageResult = new PageResult<>(responseList, total, currentPage, pageSize, (int) Math.ceil((double) total / pageSize));
                return ResponseEntity.ok().body(PageResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                List<Long> usedPromotions = productMapper.getUserUsedPromotions(userId, productId);
                // 查询当前可用的促销信息
                List<ProductPromotion> availablePromotions = productMapper.getAvailablePromotions(userId, productId);
                // 移除用户已经使用过的促销信息
                availablePromotions.removeIf(promotion -> usedPromotions.contains(promotion.getPromotion_id()));

                ProductImage main_img = productImageMapper.getProductMainImageByProductId(productId);
                productDetails.setProduct_main_image(main_img.getImage_url());
                // 筛选出最优惠的促销活动
                ProductPromotion bestPromotion = findBestPromotion(availablePromotions, productDetails.getPrice());
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
