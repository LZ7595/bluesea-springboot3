package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCart {
    private Long cart_id;
    private Long user_id;
    private Long product_id;
    private String product_name;
    private String product_main_image;
    private BigDecimal price;
    private Integer stock;
    private Integer quantity;
    private Integer is_selected;
    private BigDecimal discount_price;
}

