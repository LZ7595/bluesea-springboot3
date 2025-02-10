package com.example.backend.Entity.ccc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class BrandsController {

    @Autowired
    private BrandsService brandsService;

    @GetMapping("/brands-by-category")
    public Map<String, List<String>> getBrandsByCategory() {
        return brandsService.getBrandsByCategory();
    }
}