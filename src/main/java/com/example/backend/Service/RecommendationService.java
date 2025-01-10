package com.example.backend.Service;


import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface RecommendationService {
    List<Map<String, Object>> recommendProducts(int topN) throws IOException;
}