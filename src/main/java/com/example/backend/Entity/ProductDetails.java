package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetails {
    private Long product_id;
    private String product_name;
    private String category_name;
    private String brand_name;
    private String product_description;
    private BigDecimal price;
    private String quality;
    private int stock;
    private List<String> imageUrls;
    private List<ProductPromotion> promotions;
}