package com.example.backend.Controller;
import com.example.backend.Entity.Product;
import com.example.backend.Service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/recommend")
public class RecommendationController {
    @Autowired
    private RecommendationService recommendationService;


    @GetMapping("/{topN}")
    public ResponseEntity<List<Map<String, Object>>> recommendProducts(@PathVariable int topN) throws IOException {
        List<Map<String, Object>> recommendedProducts = recommendationService.recommendProducts(topN);
        return ResponseEntity.ok().body(recommendedProducts);
    }
}