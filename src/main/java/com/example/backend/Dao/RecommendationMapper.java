package com.example.backend.Dao;

import com.example.backend.Entity.Recommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface RecommendationMapper {

    @Select("SELECT * FROM recommendations WHERE product_id = #{productId}")
    List<Recommendation> getRecommendationsByProductId(Long productId);
}