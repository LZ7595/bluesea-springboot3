package com.example.backend.Impl;

import com.example.backend.Dao.CategoryBrandMapper;
import com.example.backend.Dao.CategoryMapper;
import com.example.backend.Entity.BrandVO;
import com.example.backend.Entity.Category;
import com.example.backend.Entity.CategoryBrandVO;
import com.example.backend.Service.CategoryBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
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

    @Override
    public CategoryBrandVO getBrandListByCategoryName(String categoryName) {
        try {
            String excludeName = null;
            List<String> categoryName1 = null;
            if ("国产".equals(categoryName)) { // 使用equals()方法比较字符串内容
                categoryName1 = Arrays.asList("手机");
                excludeName = "苹果";
            } else if( "生活".equals(categoryName)) {
                categoryName1 = Arrays.asList("运动户外", "穿戴", "摄影摄像");
            }else {
                categoryName1 = Arrays.asList(categoryName);
            }
            System.out.println("excludeName: " + categoryName1);
            List<Long> categoryIds = categoryMapper.getCategoryByName(categoryName1);
            List<BrandVO> categoryList = categoryBrandMapper.selectBrandsByCategoryId(categoryIds,excludeName);
            List<BrandVO> distinctList = categoryList.stream()
                    .filter(distinctByKey(BrandVO::getBrand_id))
                    .collect(Collectors.toList());
            // 截取前7位
            List<BrandVO> resultList = distinctList.stream()
                    .limit(7)
                    .collect(Collectors.toList());

            // 如果正好有7个，则添加"更多"选项
            if (resultList.size() == 7) {
                BrandVO moreBrand = new BrandVO();
                moreBrand.setBrand_id(000000);
                moreBrand.setBrand_name("更多");
                moreBrand.setLogo("/icon/more.png");
                moreBrand.setCategory_id(00000);
                resultList.add(moreBrand);
            }
            return new CategoryBrandVO(categoryIds.get(0).intValue(), categoryName, resultList);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    // 辅助方法：根据指定字段去重
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
