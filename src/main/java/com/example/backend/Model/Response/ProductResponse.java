package com.example.backend.Model.Response;

import com.example.backend.Model.Entity.Product;
import com.example.backend.Model.Entity.ProductPromotion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductResponse {
    private Product product;
    private ProductPromotion productPromotion;
}
