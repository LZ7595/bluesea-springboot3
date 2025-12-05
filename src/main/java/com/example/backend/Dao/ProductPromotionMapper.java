package com.example.backend.Dao;

import com.example.backend.Model.Entity.ProductPromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductPromotionMapper {
    @Select("SELECT pp.*, p.product_name, p.price " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE pp.product_id = #{productId} " +
            "AND NOW() BETWEEN pp.start_time AND pp.end_time " +
            "AND pp.status = 1")  // 新增：只查询status为1的记录
    List<ProductPromotion> getProductPromotionsByProductId(Long productId);

    @Select("SELECT pp.*, p.product_name, p.price, p.quality " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE NOW() BETWEEN pp.start_time AND pp.end_time " +
            "AND pp.status = 1 " +  // 新增：只查询status为1的记录
            "LIMIT #{num}")
    List<ProductPromotion> selectFlashSalesList(int num);

    @Select("SELECT p.*, pp.* FROM productpromotion pp JOIN product p ON pp.product_id = p.product_id " +
            "WHERE promotion_id = #{promotionId} " +
            "AND pp.status = 1")  // 新增：只查询status为1的记录
    ProductPromotion getProductPromotionById(Integer promotionId);

    /**
     * 更新优惠活动的库存（扣减已使用的优惠数量）
     * @param promotion 包含优惠ID和最新库存的实体
     */
    @Update("UPDATE productpromotion " +
            "SET promotion_stock = #{promotion_stock}, " +
            "update_time = NOW() " +
            "WHERE promotion_id = #{promotion_id}")
    void updatePromotionStock(ProductPromotion promotion);


    @Select("SELECT product_id FROM productpromotion WHERE promotion_id = #{promotionId} AND status = 1")
    List<Long> getProductIdsByPromotionId(@Param("promotionId") Long promotionId);

}