package com.example.backend.Dao;

import com.example.backend.Entity.Brand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BrandMapper {
    @Select("SELECT * FROM brand WHERE brand_id = #{brandId}")
    Brand getBrandById(Long brandId);
}