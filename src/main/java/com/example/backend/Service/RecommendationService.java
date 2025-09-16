package com.example.backend.Service;


import com.example.backend.Entity.PageResult;
import com.example.backend.Entity.ProductResponse;

import java.io.IOException;
import java.util.List;


public interface RecommendationService {
    List<ProductResponse> recommendProducts(int topN, Long targetProductIdParam) throws IOException;

    PageResult<ProductResponse> recommendProductsList(int pageNum, int pageSize,String sessionId, Long targetProductIdParam);
}