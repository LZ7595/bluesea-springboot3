package com.example.backend.Service.back;

import com.example.backend.Entity.BrandList;
import com.example.backend.Entity.Category;

import java.util.List;
import java.util.Map;

public interface CategoryBackService {
    Map<String, Object> findByParentId(Long parentId, int currentPage, int pageSize, String sortField, String sortOrder);
    String addCategory(Category category);
    String updateCategory(Category category);

    int deleteCategory(Long category_id);
    int deleteCategoryMore(List<Long> categoryIdList);
    List<BrandList> getSelectList(String keyword, Long brandId);

}
