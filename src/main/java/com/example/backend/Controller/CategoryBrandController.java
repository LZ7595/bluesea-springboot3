package com.example.backend.Controller;

import com.example.backend.Entity.CategoryBrandVO;
import com.example.backend.Service.CategoryBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categoryBrand")
public class CategoryBrandController {
    @Autowired
    private CategoryBrandService categoryBrandService;

    @GetMapping
    public List<CategoryBrandVO> getCategoryBrandList() {
        return categoryBrandService.getCategoryBrandList();
    }

    @GetMapping("/brandList")
    public CategoryBrandVO getCategoryBrandListByCategoryName(@RequestParam String categoryName, @RequestParam(defaultValue = "7") Integer limit, @RequestParam Integer type) {
        return categoryBrandService.getBrandListByCategoryName(categoryName, limit, type);
    }

    @GetMapping("/categoryList")
    public List<CategoryBrandVO> getCategoryList() {
        return categoryBrandService.getCategoryList();
    }
}
