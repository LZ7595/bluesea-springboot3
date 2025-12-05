package com.example.backend.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
// 新建实体类：用于接收按promotion_id分组的统计结果
public class PromotionUsedCountDTO {
    // 优惠ID（对应SQL的promotion_id）
    private Long promotionId;
    // 使用次数（对应SQL的used_count）
    private Integer usedCount;
}