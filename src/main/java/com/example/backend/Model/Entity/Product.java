package com.example.backend.Model.Entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private Long product_id;
    private String product_name;
    private boolean status;
    private String product_main_image;
    private Long category_id;
    private Long brand_id;
    private String quality;
    private String product_description;
    private BigDecimal price;
    private int stock;
    private int sales_volume;
    private boolean is_recommended;
    private Date create_time;
    private Date update_time;
}