package com.example.backend.Impl.back;

import com.example.backend.Dao.back.CategoryBrandBackMapper;
import com.example.backend.Model.Entity.CategoryBrand;
import com.example.backend.Service.back.CategoryBrandBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CategoryBrandBackBackServiceImpl implements CategoryBrandBackService {

    @Autowired
    private CategoryBrandBackMapper categoryBrandBackMapper;

    public Map<String, Object> Search(Long category_id, Long brand_id, int currentPage, int pageSize, String sortField, String sortOrder) {
        int offset = (currentPage - 1) * pageSize;
        List<CategoryBrand> categoryBrandList = categoryBrandBackMapper.Search(category_id, brand_id, offset, pageSize, sortField, sortOrder);
        int total = categoryBrandBackMapper.countByCategoryBrand(category_id, brand_id);
        Map<String, Object> result = Map.of("list", categoryBrandList, "total", total);
        return result;
    }

    @Override
    public int deleteOne(Long id) {
        return categoryBrandBackMapper.deleteOne(id);
    }

    @Override
    public int deleteMore(List<Long> idList) {
        return categoryBrandBackMapper.deleteMore(idList);
    }

    @Override
    public int add(CategoryBrand relation) {
        return categoryBrandBackMapper.insert(relation);
    }
    @Override
    public int update(CategoryBrand relation) {
        return categoryBrandBackMapper.update(relation);
    }
}
