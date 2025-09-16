package com.example.backend.Service;

import com.example.backend.Entity.CategoryBrandVO;

import java.util.List;

public interface CategoryBrandService {
    List<CategoryBrandVO> getCategoryBrandList();

    CategoryBrandVO getBrandListByCategoryName(String categoryName);
}
