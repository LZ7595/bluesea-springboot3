package com.example.backend.Controller;

import com.example.backend.Entity.Product;
import com.example.backend.Entity.ProductResponse;
import com.example.backend.Service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/recommend")
public class RecommendationController {
    @Autowired
    private RecommendationService recommendationService;


    @GetMapping("/{topN}")
    public ResponseEntity<List<ProductResponse>> recommendProducts(
            @PathVariable int topN,
            @RequestParam(required = false) Long targetProductId) throws IOException {
        List<ProductResponse> recommendedProducts = recommendationService.recommendProducts(topN, targetProductId);
        return ResponseEntity.ok().body(recommendedProducts);
    }
}