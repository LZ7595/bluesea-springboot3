package com.example.backend.Entity.back;

import com.example.backend.Entity.ProductDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponsePageResultBack {
    private List<ProductDetailsBack> productResponseList;
    private int total;
}
