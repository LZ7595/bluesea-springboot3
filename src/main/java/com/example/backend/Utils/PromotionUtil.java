package com.example.backend.Utils;

import com.example.backend.Model.Entity.ProductPromotion;
import com.example.backend.Model.Dto.ProductPromotionWrapper;
import com.example.backend.Model.Dto.UnusablePromotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 促销处理工具类：封装促销可用/不可用分类、不可用原因生成等核心逻辑
 */
public class PromotionUtil {

    // 工具类日志（使用当前工具类作为日志名，便于定位问题）
    private static final Logger log = LoggerFactory.getLogger(PromotionUtil.class);

    /**
     * 区分已登录用户的可用/不可用促销（核心逻辑）
     * @param allPromotions 商品的所有促销列表（可能为null）
     * @param usedCountMap 用户促销使用次数映射（promotionId -> usedCount，可能为null）
     * @param productStock 商品基础库存
     * @param wrapper 存储结果的促销包装类（需提前创建传入）
     */
    public static void splitUsableAndUnusablePromotions(
            List<ProductPromotion> allPromotions,
            Map<Long, Integer> usedCountMap,
            int productStock,
            ProductPromotionWrapper wrapper) {

        // 安全处理：避免空指针
        List<ProductPromotion> safePromotions = allPromotions == null ? Collections.emptyList() : allPromotions;
        Map<Long, Integer> safeUsedCountMap = usedCountMap == null ? Collections.emptyMap() : usedCountMap;

        List<ProductPromotion> usable = new ArrayList<>();
        List<UnusablePromotion> unusable = new ArrayList<>();

        // 日志：输入数据概况
        log.info("开始处理促销分类：促销总数={}，商品基础库存={}，用户已用促销数={}",
                safePromotions.size(), productStock, safeUsedCountMap.size());

        for (ProductPromotion promotion : safePromotions) {
            Long promotionId = promotion.getPromotion_id();
            // 安全获取促销参数（避免字段为null）
            int perUserLimit = promotion.getPer_user_limit() == null ? 0 : promotion.getPer_user_limit();
            int usedCount = safeUsedCountMap.getOrDefault(promotionId, 0);
            int availableCount = perUserLimit - usedCount;
            int promotionStock = promotion.getPromotion_quantity() == null ? 0 : promotion.getPromotion_quantity();

            // 日志：单个促销关键参数
            log.info("促销ID={}：限购数={}，已用次数={}，剩余可用次数={}，活动库存={}",
                    promotionId, perUserLimit, usedCount, availableCount, promotionStock);

            // 计算折扣价（工具类依赖外部计算器，保持原有逻辑）
            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
            promotion.setDiscount_price(discountPrice);

            // 判断可用状态并分类
            if (availableCount > 0 && promotionStock > 0 && productStock > 0) {
                usable.add(promotion);
                log.info("促销ID={}：归类为【可用】", promotionId);
            } else {
                String reason = getUnusableReason(availableCount, promotionStock, productStock);
                unusable.add(new UnusablePromotion(promotion, reason, usedCount, availableCount));
                log.info("促销ID={}：归类为【不可用】，原因：{}", promotionId, reason);
            }
        }

        // 填充结果到包装类
        wrapper.setUsablePromotions(usable);
        wrapper.setUnusablePromotions(unusable);

        // 日志：分类结果
        log.info("促销分类完成：可用促销数={}，不可用促销数={}", usable.size(), unusable.size());
    }

    /**
     * 生成促销不可用的具体原因（顺序：限购次数 -> 活动库存 -> 商品库存）
     * @param availableCount 用户剩余可用次数
     * @param promotionStock 促销活动库存
     * @param productStock 商品基础库存
     * @return 不可用原因文案
     */
    public static String getUnusableReason(int availableCount, int promotionStock, int productStock) {
        if (availableCount <= 0) {
            return "已达每人限购次数";
        }
        if (promotionStock <= 0) {
            return "活动库存不足";
        }
        if (productStock <= 0) {
            return "商品库存不足";
        }
        return "促销暂不可用";
    }

    /**
     * 过滤未登录用户的可用促销（仅判断活动库存和商品库存，复用原逻辑）
     * @param promotions 未登录用户的促销列表
     * @param productStock 商品基础库存
     * @return 可用促销列表
     */
    public static List<ProductPromotion> filterAnonymousUsablePromotions(List<ProductPromotion> promotions, int productStock) {
        if (promotions == null || promotions.isEmpty()) {
            log.info("未登录用户促销过滤：输入促销为空，返回空列表");
            return Collections.emptyList();
        }

        List<ProductPromotion> usable = promotions.stream()
                .filter(promotion -> {
                    // 安全判断：避免字段为null
                    int promotionStock = promotion.getPromotion_quantity() == null ? 0 : promotion.getPromotion_quantity();
                    boolean stockValid = promotionStock > 0 && productStock > 0;
                    log.info("未登录用户促销ID={}：活动库存={}，商品库存={}，库存有效={}",
                            promotion.getPromotion_id(), promotionStock, productStock, stockValid);
                    return stockValid;
                })
                .peek(promotion -> {
                    // 计算折扣价
                    BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(promotion);
                    promotion.setDiscount_price(discountPrice);
                    log.info("未登录用户促销ID={}：计算折扣价={}", promotion.getPromotion_id(), discountPrice);
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        log.info("未登录用户促销过滤完成：可用促销数={}", usable.size());
        return usable;
    }
}