package com.example.backend.Entity.back;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductInPromotion {
    private Long product_id;
    private String product_main_image;
    private String product_name;
    private BigDecimal price;
    private String quality;
    private Integer stock;
}
