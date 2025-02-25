package com.example.backend.Dao;

import com.example.backend.Entity.ProductPromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductPromotionMapper {
    @Select("SELECT pp.*, p.product_name, p.price " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE pp.product_id = #{productId} " +
            "AND NOW() BETWEEN pp.start_time AND pp.end_time ")
    List<ProductPromotion> getProductPromotionsByProductId(Long productId);

    @Select("SELECT pp.*, p.product_name, p.price, p.quality " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE NOW() BETWEEN pp.start_time AND pp.end_time " +
            "LIMIT #{num}")
    List<ProductPromotion> selectFlashSalesList(int num);
}