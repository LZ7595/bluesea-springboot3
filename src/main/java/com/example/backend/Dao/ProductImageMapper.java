package com.example.backend.Dao;

import com.example.backend.Entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductImageMapper {
    @Select("SELECT * FROM productimage WHERE product_id = #{productId} ORDER BY is_main_image DESC")
    List<ProductImage> getProductImagesByProductId(Long productId);

    @Select("SELECT * FROM productimage WHERE product_id = #{productId} AND is_main_image = true")
    ProductImage getProductMainImageByProductId(Long productId);
}