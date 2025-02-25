package com.example.backend.Impl.back;

import com.example.backend.Dao.*;
import com.example.backend.Dao.back.PromotionBackMapper;
import com.example.backend.Entity.*;
import com.example.backend.Entity.back.ProductInPromotion;
import com.example.backend.Entity.back.PromotionBack;
import com.example.backend.Entity.back.PromotionResponsePageResultBack;
import com.example.backend.Service.back.PromotionBackService;
import com.example.backend.Utils.PromotionDiscountCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PromotionBackServiceImpl implements PromotionBackService {

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private PromotionBackMapper promotionBackMapper;

    public ResponseEntity<PromotionResponsePageResultBack> SearchPromotionList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize) {
        try {
            System.out.println(searchKeyword);
            Map<String, Object> params = new HashMap<>();
            params.put("searchKeyword", searchKeyword);
            params.put("sortField", sortField);
            params.put("sortOrder", sortOrder);
            params.put("offset", (currentPage - 1) * pageSize);
            params.put("pageSize", pageSize);
            List<PromotionBack> promotionList = promotionBackMapper.SearchPromotionList(params);
            Map<String, Object> countParams = new HashMap<>();
            countParams.put("searchKeyword", searchKeyword);
            int total = promotionBackMapper.getSearchPromotionTotal(countParams);
            if (promotionList != null) {
                List<PromotionBack> responseList = promotionList.stream().map(promotion -> {
                    // 获取产品ID
                    Long productId = promotion.getProduct_id();
                    if (promotion == null) {
                        throw new RuntimeException("Promotion not found with id: " + productId);
                    }

                    ProductImage productMainImage = productImageMapper.getProductMainImageByProductId(productId);
                    promotion.setProduct_main_image(productMainImage.getImage_url());


                    BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
                    // 设置计算得到的折扣价格
                    promotion.setDiscount_price(discountPrice);

                    return promotion;
                }).collect(Collectors.toList());
                PromotionResponsePageResultBack promotionResponsePageResultBack = new PromotionResponsePageResultBack(responseList, total);
                return ResponseEntity.ok().body(promotionResponsePageResultBack);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    public ResponseEntity<PromotionBack> getPromotionDetailsBack(Long promotionId) {
        try {
            PromotionBack promotion = promotionBackMapper.getPromotionDetailBackById(promotionId);
            if (promotion != null) {
                ProductImage productMainImage = productImageMapper.getProductMainImageByProductId(promotion.getProduct_id());
                promotion.setProduct_main_image(productMainImage.getImage_url());
                return ResponseEntity.ok().body(promotion);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }


    public ResponseEntity<?> getProductDetailBack(Long productId) {
        try {
            ProductInPromotion product = promotionBackMapper.getProductBack(productId);
        if (product != null) {
            ProductImage productMainImage = productImageMapper.getProductMainImageByProductId(productId);
            product.setProduct_main_image(productMainImage.getImage_url());
            return ResponseEntity.ok().body(product);
        } else {
            return ResponseEntity.status(404).body(null);
        }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }

    }

    @Override
    public int updatePromotion(PromotionBack promotion) {
        try {
            Long promotionId = promotion.getPromotion_id();

            // 处理促销活动
            handlePromotions(promotionId, promotion);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            handleException(e);
            return 0;
        }
    }

    @Override
    public int addPromotion(PromotionBack promotion) {
        try {
            // 插入商品记录
            int result = promotionBackMapper.insertPromotion(promotion);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int deletePromotion(Long promotionId) {
        return promotionBackMapper.deletePromotion(promotionId);
    }

    @Override
    public int deletePromotionMore(List<Long> promotionIdList) {
        return promotionBackMapper.deletePromotionMore(promotionIdList);
    }

    private void handlePromotions(Long promotionId, PromotionBack newPromotion) {
        System.out.println("处理产品ID为 " + promotionId + " 的促销信息");
        System.out.println("新的促销信息: " + newPromotion);
        if (newPromotion == null) {
            return;
        }

        PromotionBack existingPromotions = promotionBackMapper.getPromotionDetailBackById(promotionId);

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

    private PromotionBack findExistingPromotion(PromotionBack existingPromotions, Long promotionId) {
        if (existingPromotions.getPromotion_id().equals(promotionId)) {
            return existingPromotions;
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
