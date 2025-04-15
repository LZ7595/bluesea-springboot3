package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPayInfo {
    private Long product_id;
    private String product_name;
    private BigDecimal price;
    private String product_main_image;
    private List<ProductPromotion> promotions;
}
