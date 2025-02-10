package com.example.backend.Dao;

import com.example.backend.Entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductReviewMapper {
    @Select("SELECT * FROM productreview WHERE product_id = #{productId}")
    List<ProductReview> getReviewsByProductId(Long productId);
}
