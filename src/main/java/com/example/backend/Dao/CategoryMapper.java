package com.example.backend.Dao;

import com.example.backend.Entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper {
    @Select("SELECT * FROM category WHERE category_id = #{categoryId}")
    Category getCategoryById(Long categoryId);
}