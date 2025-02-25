package com.example.backend.Entity.back;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromotionBack {
    private Long promotion_id;
    private Long product_id;
    private String product_main_image;
    private String promotion_type;
    private Date start_time;
    private Date end_time;
    private BigDecimal discount_rate;
    private BigDecimal reduce_amount;
    private String product_name;
    private BigDecimal price;
    private String quality;
    private BigDecimal discount_price;
    private Date create_time; // 新增的创建时间字段
    private Date update_time; // 新增的更新时间字段
}
