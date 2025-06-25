package com.example.backend.Dao.back;

import com.example.backend.Entity.CategoryBrand;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryBrandBackMapper {
    @Select("<script>" +
            "SELECT " +
            "    cb.*, " +
            "    b.brand_name, b.logo, " +
            "    c.category_name " +
            "FROM brand_category_relation cb " +
            "LEFT JOIN brand b ON cb.brand_id = b.brand_id " +
            "LEFT JOIN category c ON cb.category_id = c.category_id " +
            "<where>" +
            "    <if test=\"category_id != null\">AND cb.category_id = #{category_id}</if>" +
            "    <if test=\"brand_id != null\">AND cb.brand_id = #{brand_id}</if>" +
            "</where>" +
            "<choose>" +
            "    <when test=\"sortField != null and sortField != ''\">" +
            "        ORDER BY ${sortField} ${sortOrder}" +
            "    </when>" +
            "    <otherwise>" +
            "        ORDER BY cb.category_id ASC" +
            "    </otherwise>" +
            "</choose>" +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<CategoryBrand> Search(Long category_id, Long brand_id, int offset, int pageSize, String sortField, String sortOrder);

    @Select("<script>" +
            "SELECT COUNT(id) " +
            "FROM brand_category_relation " +
            "<where>" +
            "    <if test=\"category_id != null\">AND category_id = #{category_id}</if>" +
            "    <if test=\"brand_id != null\">AND brand_id = #{brand_id}</if>" +
            "</where>" +
            "</script>")
    int countByCategoryBrand(Long category_id, Long brand_id);

    @Update("UPDATE brand_category_relation SET status = false WHERE id = #{id}")
    int deleteOne(Long id);


    @Update("<script>" +
            "UPDATE brand_category_relation " +
            "SET status = false " +
            "WHERE id IN " +
            "<foreach item='item' index='index' collection='idList' " +
            "open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int deleteMore(@Param("idList") List<Long> idList);

    @Insert("INSERT INTO brand_category_relation (brand_id, category_id, status, create_time, update_time) " +
            "VALUES (#{brand_id}, #{category_id}, #{status}, NOW(), NOW())")
    int insert(CategoryBrand relation);
    @Update("UPDATE brand_category_relation SET brand_id = #{brand_id}, category_id = #{category_id}, status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(CategoryBrand relation);
}
