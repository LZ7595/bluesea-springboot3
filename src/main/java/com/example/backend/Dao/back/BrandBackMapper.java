package com.example.backend.Dao.back;

import com.example.backend.Entity.Brand;
import com.example.backend.Entity.BrandList;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper

public interface BrandBackMapper {

    @Select("<script>" +
            "SELECT * FROM brand " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (brand_name LIKE CONCAT('%', #{searchKeyword}, '%') OR brand_id LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Brand> SearchBrandList(Map<String, Object> params);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM brand " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (brand_name LIKE CONCAT('%', #{searchKeyword}, '%') OR brand_id LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "</script>")
    int getSearchBrandTotal(Map<String, Object> params);

    @Update("UPDATE brand " +
            "SET brand_name = #{brand.brand_name}, " +
            "    status = #{brand.status}, " +
            "    brand_description = #{brand.brand_description}, " +
            "    logo = #{brand.logo}, " +
            "    update_time = NOW() " +
            "WHERE brand_id = #{brand.brand_id}")
    int updateBrand(@Param("brand") Brand brand);



    @Insert("INSERT INTO brand (brand_name, status, brand_description, logo, create_time, update_time) " +
            "SELECT #{brand.brand_name}, #{brand.status}, #{brand.brand_description}, #{brand.logo}, NOW(), NOW() ")
    int addBrand(@Param("brand") Brand brand);

    @Delete("DELETE FROM brand WHERE brand_id = #{brandId}")
    int deleteBrand(Long brandId);


    @Delete("<script>" +
            "DELETE FROM brand " +
            "WHERE brand_id IN " +
            "<foreach item='item' index='index' collection='brandIdList' " +
            "open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int deleteBrandMore(@Param("brandIdList") List<Long> brandIdList);

    @Select("<script>" +
            "SELECT brand_name AS label, brand_id AS value FROM brand " +
            "<where>" +
            "    <if test='keyword != null and keyword != \"\"'>" +
            "        brand_name LIKE CONCAT('%', #{keyword}, '%')" +
            "    </if>" +
            "</where>" +
            "ORDER BY brand_id ASC " +
            "</script>")
    List<BrandList> getBrandList(@Param("keyword") String keyword);
}
