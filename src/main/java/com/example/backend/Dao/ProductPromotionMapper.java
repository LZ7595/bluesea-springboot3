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

    @Select("SELECT p.*, pp.* FROM productpromotion pp JOIN product p ON pp.product_id = p.product_id WHERE promotion_id = #{promotionId}")
    ProductPromotion getProductPromotionById(Integer promotionId);

    /**
     * 更新优惠活动的库存（扣减已使用的优惠数量）
     * @param promotion 包含优惠ID和最新库存的实体
     */
    @Update("UPDATE productpromotion " +
            "SET promotion_stock = #{promotion_stock}, " +
            "update_time = NOW() " +  // 假设存在更新时间字段，自动更新
            "WHERE promotion_id = #{promotion_id}")
    void updatePromotionStock(ProductPromotion promotion);
}