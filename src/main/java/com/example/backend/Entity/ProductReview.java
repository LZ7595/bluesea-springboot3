package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductReview {
    private Long review_id;
    private Long product_id;
    private Long user_id;
    private int rating;
    private String review_content;
    private List<String> imageUrls;
    private Date review_time;
}