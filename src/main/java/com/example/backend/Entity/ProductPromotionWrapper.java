package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 促销包装类：包含一个商品的可用和不可用促销信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPromotionWrapper {
    // 可用促销列表
    private List<ProductPromotion> usablePromotions;
    // 不可用促销列表（含原因）
    private List<UnusablePromotion> unusablePromotions;
}
