package com.example.backend.Entity.back;

import com.example.backend.Entity.ProductPromotion;
import com.example.backend.Entity.ProductReview;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailsBack {
    private Long product_id;
    private String product_name;
    private boolean status;
    private String category_name;
    private String brand_name;
    private String product_description;
    private BigDecimal price;
    private String quality;
    private int stock;
    private boolean recommended;
    private int sales_volume;
    private Date create_time;
    private Date update_time;
    private List<String> imageUrls;
    private List<PromotionBack> promotions;
    private List<ProductReview> reviews;
}
