package com.example.backend.Service.back;

import com.example.backend.Model.Entity.CategoryBrand;

import java.util.List;
import java.util.Map;

public interface CategoryBrandBackService {

    Map<String, Object> Search(Long category_id, Long brand_id, int currentPage, int pageSize, String sortField, String sortOrder);
    int deleteOne(Long id);
    int deleteMore(List<Long> idList);
    int add(CategoryBrand relation);
    int update(CategoryBrand relation);
}
