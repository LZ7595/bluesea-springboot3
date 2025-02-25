package com.example.backend.Utils;

import com.example.backend.Entity.ProductPromotion;
import com.example.backend.Entity.back.PromotionBack;

import java.math.BigDecimal;

public class PromotionDiscountCalculator {

    public static BigDecimal calculateDiscountPrice(ProductPromotion promotion) {
        BigDecimal price = promotion.getPrice();
        String promotionType = promotion.getPromotion_type();
        BigDecimal discountRate = promotion.getDiscount_rate();
        BigDecimal reduceAmount = promotion.getReduce_amount();

        BigDecimal discountPrice;
        switch (promotionType) {
            case "DISCOUNT":
                // 折扣类型，折扣价格 = 原价 * 折扣率
                discountPrice = price.multiply(discountRate);
                break;
            case "REDUCE_AMOUNT":
                // 满减类型，折扣价格 = 原价 - 满减金额
                discountPrice = price.subtract(reduceAmount);
                break;
            case "BUY_ONE_GET_ONE":
                // 买一送一类型，折扣价格 = 原价 / 2
                discountPrice = price.divide(new BigDecimal("2"));
                break;
            case "FREE_SHIPPING":
                // 免运费类型，折扣价格等于原价
                discountPrice = price;
                break;
            default:
                // 未知促销类型，折扣价格等于原价
                discountPrice = price;
        }
        return discountPrice;
    }

    public static BigDecimal calculateDiscountPrice(PromotionBack promotion) {
        BigDecimal price = promotion.getPrice();
        String promotionType = promotion.getPromotion_type();
        BigDecimal discountRate = promotion.getDiscount_rate();
        BigDecimal reduceAmount = promotion.getReduce_amount();

        BigDecimal discountPrice;
        switch (promotionType) {
            case "DISCOUNT":
                // 折扣类型，折扣价格 = 原价 * 折扣率
                discountPrice = price.multiply(discountRate);
                break;
            case "REDUCE_AMOUNT":
                // 满减类型，折扣价格 = 原价 - 满减金额
                discountPrice = price.subtract(reduceAmount);
                break;
            case "BUY_ONE_GET_ONE":
                // 买一送一类型，折扣价格 = 原价 / 2
                discountPrice = price.divide(new BigDecimal("2"));
                break;
            case "FREE_SHIPPING":
                // 免运费类型，折扣价格等于原价
                discountPrice = price;
                break;
            default:
                // 未知促销类型，折扣价格等于原价
                discountPrice = price;
        }
        return discountPrice;
    }
}