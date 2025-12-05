package com.example.backend.Service;

import com.example.backend.Model.Vo.CategoryBrandVO;

import java.util.List;

public interface CategoryBrandService {
    List<CategoryBrandVO> getCategoryBrandList();

    CategoryBrandVO getBrandListByCategoryName(String categoryName,Integer limit, Integer type);

    List<CategoryBrandVO> getCategoryList();
}
