package com.example.backend.Impl;

import com.example.backend.Dao.CategoryBrandMapper;
import com.example.backend.Dao.CategoryMapper;
import com.example.backend.Entity.BrandVO;
import com.example.backend.Entity.CategoryBrandVO;
import com.example.backend.Service.CategoryBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryBrandServiceImpl implements CategoryBrandService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;
    @Override
    public List<CategoryBrandVO> getCategoryBrandList() {
        // 查询所有分类
        List<CategoryBrandVO> categoryList = categoryMapper.selectAll();

        System.out.println(categoryList.stream().map(CategoryBrandVO::getCategory_id).collect(Collectors.toList()));

        // 查询所有关联关系
        Map<Integer, List<BrandVO>> brandMap = categoryBrandMapper.selectBrandMapByCategoryIds(
                categoryList.stream().map(CategoryBrandVO::getCategory_id).collect(Collectors.toList())
        );

        // 填充品牌信息
        for (CategoryBrandVO category : categoryList) {
            List<BrandVO> brands = brandMap.getOrDefault(category.getCategory_id(), Collections.emptyList());
            category.setBrands(brands);
        }

        return categoryList;
    }
}
