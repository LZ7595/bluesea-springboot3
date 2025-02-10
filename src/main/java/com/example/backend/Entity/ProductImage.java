package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImage {
    private Long image_id;
    private Long product_id;
    private String image_url;
    private boolean is_main_image;
}
