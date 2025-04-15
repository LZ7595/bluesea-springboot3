package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long item_id;
    private Long order_id;
    private Long product_id;
    private Integer promotion_id;
    private ProductPayInfo product;
    private Integer quantity;
    private BigDecimal unit_price;
    private BigDecimal discount_amount;
    private BigDecimal total_price;
}
