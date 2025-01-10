package com.example.backend.Entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private Long product_id;
    private String product_name;
    private String description;
    private double price;
    private int category_id;
    private String quality;
    private int stock;
    private String brand;
    private String image_url;
}