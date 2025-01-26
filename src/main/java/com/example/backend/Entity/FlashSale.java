package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlashSale {
    private int flash_sale_id;
    private int product_id;
    private String product_name;
    private double price;
    private double discount_price;
    private int max_quantity;
    private int sold_quantity;
    private String image_url;
    private String quality;
}
