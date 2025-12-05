package com.example.backend.Model.Dto;

import com.example.backend.Model.Entity.ProductPromotion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 不可用促销信息：包含促销基本信息、不可用原因和使用次数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnusablePromotion {
    // 促销基本信息
    private ProductPromotion promotion;
    // 不可用原因（如"已达限购次数"、"活动库存不足"）
    private String reason;
    // 用户已使用次数
    private int usedCount;
    // 剩余可用次数（可能为0或负数）
    private int availableCount;
}
