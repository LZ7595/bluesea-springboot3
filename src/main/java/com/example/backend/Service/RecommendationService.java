package com.example.backend.Service;


import com.example.backend.Entity.ProductResponse;

import java.io.IOException;
import java.util.List;


public interface RecommendationService {
    List<ProductResponse> recommendProducts(int topN) throws IOException;
}