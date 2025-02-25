package com.example.backend.Impl.back;

import com.example.backend.Dao.*;
import com.example.backend.Dao.back.ProductBackMapper;
import com.example.backend.Dao.back.PromotionBackMapper;
import com.example.backend.Entity.*;
import com.example.backend.Entity.back.ProductDetailsBack;
import com.example.backend.Entity.back.ProductResponsePageResultBack;
import com.example.backend.Entity.back.PromotionBack;
import com.example.backend.Service.back.ProductBackService;
import com.example.backend.Utils.PromotionDiscountCalculator;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ProductBackServiceImpl implements ProductBackService {
    @Autowired
    private ProductBackMapper productBackMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private PromotionBackMapper promotionBackMapper;
    @Autowired
    private ProductReviewMapper productReviewMapper;

    public ResponseEntity<ProductResponsePageResultBack> SearchProductList(String selectedCategory, String selectedBrand, String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize) {
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
            List<Product> productList = productBackMapper.SearchProductList(params);
            Map<String, Object> countParams = new HashMap<>();
            countParams.put("selectedCategory", selectedCategory);
            countParams.put("selectedBrand", selectedBrand);
            countParams.put("searchKeyword", searchKeyword);
            int total = productBackMapper.getSearchProductTotal(countParams);
            if (productList != null) {
                List<ProductDetailsBack> responseList = productList.stream().map(product -> {
                    // 获取产品ID
                    Long productId = product.getProduct_id();
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

                    List<PromotionBack> promotions = promotionBackMapper.getPromotionsByProductId(productId);
                    for (PromotionBack promotion : promotions) {
                        BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
                        // 设置计算得到的折扣价格
                        promotion.setDiscount_price(discountPrice);
                    }
                    List<ProductReview> reviews = productReviewMapper.getReviewsByProductId(productId);
                    return new ProductDetailsBack(
                            product.getProduct_id(),
                            product.getProduct_name(),
                            product.isStatus(),
                            category.getCategory_name(),
                            brand.getBrand_name(),
                            product.getProduct_description(),
                            product.getPrice(),
                            product.getQuality(),
                            product.getStock(),
                            product.is_recommended(),
                            product.getSales_volume(),
                            product.getCreate_time(),
                            product.getUpdate_time(),
                            imageUrls,
                            promotions,
                            reviews
                    );
                }).collect(Collectors.toList());
                ProductResponsePageResultBack productResponsePageResultBack = new ProductResponsePageResultBack(responseList, total);
                return ResponseEntity.ok().body(productResponsePageResultBack);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    public ResponseEntity<ProductDetailsBack> getProductDetailsBack(Long productId) {
        try {
            ProductDetailsBack productDetail = productBackMapper.getProductDetailsBackById(productId);
            System.out.println(productDetail);
            if (productDetail != null) {
                List<ProductImage> productImages = productImageMapper.getProductImagesByProductId(productId);
                List<String> imageUrls = new ArrayList<>();
                for (ProductImage image : productImages) {
                    imageUrls.add(image.getImage_url());
                }
                productDetail.setImageUrls(imageUrls);

                List<PromotionBack> promotions = promotionBackMapper.getPromotionsByProductId(productId);
                productDetail.setPromotions(promotions);
                return ResponseEntity.ok().body(productDetail);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public int updateProduct(ProductDetailsBack product) {
        try {
            Long productId = product.getProduct_id();

            // 处理图片链接
            handleImageUrls(productId, product.getImageUrls());

            // 处理促销活动
            handlePromotions(productId, product.getPromotions());

            // 更新产品信息
            int rowsAffected = productBackMapper.updateProduct(product);
            System.out.println("更新产品信息，受影响的行数: " + rowsAffected);

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            handleException(e);
            return 0;
        }
    }

    @Override
    public int addProduct(ProductDetailsBack product) {
        try {
            // 插入商品记录
            int result = productBackMapper.addProduct(product);
            if (result > 0) {
                // 获取插入记录后生成的 product_id
                Long productId = product.getProduct_id();
                // 获取商品的图片链接列表
                List<String> imageUrls = product.getImageUrls();
                // 插入图片链接
                for (String imageUrl : imageUrls) {
                    productBackMapper.insertProductImage(imageUrl, productId);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int deleteProduct(Long productId) {
        return productBackMapper.deleteProduct(productId);
    }

    @Override
    public int deleteProductMore(List<Long> productIdList) {
        return productBackMapper.deleteProductMore(productIdList);
    }

    @Override
    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }
        // 生成唯一文件名
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String filePath = "G:\\毕业设计\\Blue - Ocean Technology(H5)\\public\\goods\\" + fileName; // 上传文件保存路径
        File dest = new File(filePath);
        String fileUrl = "/goods/" + fileName; // 文件访问路径
        try {
            file.transferTo(dest);
            return fileUrl;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void handleImageUrls(Long productId, List<String> newUrls) {
        // 获取当前产品已有的图片链接
        List<String> existingUrls = productBackMapper.getImageUrlsByProductId(productId);

        // 找出需要新增和删除的链接
        List<String> toAdd = new ArrayList<>();
        List<String> toDelete = new ArrayList<>();

        for (String newUrl : newUrls) {
            if (!existingUrls.contains(newUrl)) {
                toAdd.add(newUrl);
            }
        }

        for (String existingUrl : existingUrls) {
            if (!newUrls.contains(existingUrl)) {
                toDelete.add(existingUrl);
            }
        }

        // 插入新增的图片链接
        for (String imageUrl : toAdd) {
            productBackMapper.insertProductImage(imageUrl, productId);
        }

        // 删除需要移除的图片链接记录
        for (String imageUrl : toDelete) {
            productBackMapper.deleteProductImage(imageUrl, productId);
        }

        System.out.println("需要新增的图片链接: " + toAdd);
        System.out.println("需要删除的图片链接: " + toDelete);
    }

    private void handlePromotions(Long productId, List<PromotionBack> newPromotions) {
        System.out.println("处理产品ID为 " + productId + " 的促销信息");
        System.out.println("新的促销信息: " + newPromotions);
        if (newPromotions == null || newPromotions.isEmpty()) {
            return;
        }

        List<PromotionBack> existingPromotions = promotionBackMapper.getPromotionsByProductId(productId);

        for (PromotionBack newPromotion : newPromotions) {
            if (newPromotion.getPromotion_id() == null) {
                System.out.println("新增促销信息: " + newPromotion);
                promotionBackMapper.insertPromotion(newPromotion);
            } else {
                PromotionBack existingPromotion = findExistingPromotion(existingPromotions, newPromotion.getPromotion_id());
                if (existingPromotion != null && shouldUpdatePromotion(newPromotion, existingPromotion)) {
                    promotionBackMapper.updatePromotion(newPromotion);
                }
            }
        }
    }

    private PromotionBack findExistingPromotion(List<PromotionBack> existingPromotions, Long promotionId) {
        for (PromotionBack promotion : existingPromotions) {
            if (promotion.getPromotion_id().equals(promotionId)) {
                return promotion;
            }
        }
        return null;
    }

    private boolean shouldUpdatePromotion(PromotionBack newPromotion, PromotionBack existingPromotion) {
        boolean typeChanged = newPromotion.getPromotion_type() == null ?
                existingPromotion.getPromotion_type() != null :
                !newPromotion.getPromotion_type().equals(existingPromotion.getPromotion_type());

        boolean timeChanged = !isSameDate(newPromotion.getStart_time(), existingPromotion.getStart_time()) ||
                !isSameDate(newPromotion.getEnd_time(), existingPromotion.getEnd_time());

        boolean amountOrRateChanged = false;
        if ("REDUCE_AMOUNT".equals(existingPromotion.getPromotion_type())) {
            amountOrRateChanged = !isSameBigDecimal(newPromotion.getReduce_amount(), existingPromotion.getReduce_amount());
        } else if ("DISCOUNT".equals(existingPromotion.getPromotion_type())) {
            amountOrRateChanged = !isSameBigDecimal(newPromotion.getDiscount_rate(), existingPromotion.getDiscount_rate());
        }

        return typeChanged || timeChanged || amountOrRateChanged;
    }

    private boolean isSameDate(Date date1, Date date2) {
        return date1 != null && date2 != null && date1.equals(date2);
    }

    private boolean isSameBigDecimal(BigDecimal bd1, BigDecimal bd2) {
        return (bd1 == null && bd2 == null) || (bd1 != null && bd2 != null && bd1.compareTo(bd2) == 0);
    }

    private void handleException(Exception e) {
        if (e instanceof org.springframework.dao.DataAccessException) {
            System.out.println("数据库访问异常: " + e.getMessage());
        } else {
            System.out.println("其他异常: " + e.getMessage());
        }
    }
}
