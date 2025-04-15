package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductReview {
    private Integer review_id;
    private Integer order_id;
    private Integer product_id;
    private String product_name;
    private Integer user_id;
    private String username;
    private String avatar;
    private BigDecimal rating;
    private String review_content;
    private Date review_time;
    private String image_urls;
    private boolean status;
}