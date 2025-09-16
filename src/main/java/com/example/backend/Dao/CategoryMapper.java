package com.example.backend.Dao;

import com.example.backend.Entity.Category;
import com.example.backend.Entity.CategoryBrandVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {
    @Select("SELECT * FROM category WHERE category_id = #{categoryId}")
    Category getCategoryById(Long categoryId);

    @Select({
            "<script>",
            "SELECT category_id FROM category",
            "WHERE category_name IN",
            "<foreach item='name' collection='list' open='(' separator=',' close=')'>",
            "#{name}",
            "</foreach>",
            "</script>"
    })
    List<Long> getCategoryByName(List<String> categoryNames);

    @Select("SELECT category_id, category_name FROM category WHERE status = 1 ORDER BY order_num")
    List<CategoryBrandVO> selectAll();
}