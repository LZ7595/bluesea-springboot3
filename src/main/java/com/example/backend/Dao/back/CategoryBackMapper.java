package com.example.backend.Dao.back;

import com.example.backend.Entity.BrandList;
import com.example.backend.Entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryBackMapper {

    @Select("<script>" +
            "SELECT * FROM category WHERE parent_category_id = #{parentId} " +
            "<if test='sortField != \"\"'>" +
            "ORDER BY ${sortField} ${sortOrder}" +
            "</if>" +
            " LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Category> selectByParentId(@Param("parentId") Long parentId, @Param("offset") int offset, @Param("pageSize") int pageSize, @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);

    @Select("SELECT COUNT(category_id) FROM category WHERE parent_category_id = #{parentId}")
    Integer countByParentId(Long parentId);

    @Select("SELECT COUNT(category_id) FROM category WHERE parent_category_id = #{parentId}")
    Integer countAllByParentId(Long parentId);

    @Insert("INSERT INTO category (category_name, parent_category_id, status, order_num, image_url) VALUES (#{category_name}, #{parent_category_id}, #{status}, #{order_num}, #{image_url})")
    int insert(Category category);

    @Update("UPDATE category SET category_name=#{category_name}, parent_category_id=#{parent_category_id}, status=#{status}, order_num=#{order_num}, image_url=#{image_url} WHERE category_id=#{category_id}")
    int update(Category category);

    @Delete("DELETE FROM category WHERE category_id = #{category_id}")
    int deleteCategory(Long category_id);


    @Delete("<script>" +
            "DELETE FROM category " +
            "WHERE category_id IN " +
            "<foreach item='item' index='index' collection='categoryIdList' " +
            "open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int deleteCategoryMore(@Param("categoryIdList") List<Long> categoryIdList);

    @Select("<script>" +
            "SELECT category_name AS label, category_id AS value FROM category " +
            "<where>" +
            "    <if test='keyword != null and keyword != \"\"'>" +
            "        category_name LIKE CONCAT('%', #{keyword}, '%')" +
            "    </if>" +
            "</where>" +
            "ORDER BY category_id ASC " +
            "</script>")
    List<BrandList> getCategoryList(@Param("keyword") String keyword);
}
