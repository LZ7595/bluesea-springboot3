package com.example.backend.Controller;

import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Response.ProductResponse;
import com.example.backend.Service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/product/recommend")
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

    @GetMapping("/list")
    public ResponseEntity<PageResult<ProductResponse>> recommendProductsList(@RequestParam int pageNum, @RequestParam int pageSize, @RequestParam String sessionId, @RequestParam(required = false) Long targetProductId) throws IOException {
        PageResult<ProductResponse> recommendedProducts = recommendationService.recommendProductsList(pageNum, pageSize, sessionId, targetProductId);
        System.out.println(recommendedProducts);

        return ResponseEntity.ok().body(recommendedProducts);
    }
}