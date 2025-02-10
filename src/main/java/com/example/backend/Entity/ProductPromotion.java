package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPromotion {
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
}
