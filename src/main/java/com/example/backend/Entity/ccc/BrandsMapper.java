package com.example.backend.Entity.ccc;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrandsMapper {
    @Select("SELECT b.brand_name " +
            "FROM brand b " +
            "JOIN brand_category_relation bcr ON b.brand_id = bcr.brand_id " +
            "JOIN category c ON bcr.category_id = c.category_id " +
            "WHERE c.category_name = #{categoryName}")
    List<Brands> getBrandsByCategoryName(String categoryName);

    @Select("SELECT category_name FROM category")
    List<String> getAllCategoryNames();
}