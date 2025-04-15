package com.example.backend.Dao.back;

import com.example.backend.Entity.back.ProductInPromotion;
import com.example.backend.Entity.back.PromotionBack;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper

public interface PromotionBackMapper {

    @Select("<script>" +
            "SELECT pp.*, p.product_name, p.price, p.quality " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR p.product_id LIKE CONCAT('%', #{searchKeyword}, '%') OR pp.promotion_id LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<PromotionBack> SearchPromotionList(Map<String, Object> params);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR p.promotion_id LIKE CONCAT('%', #{searchKeyword}, '%') OR p.promotion_id LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "</script>")
    int getSearchPromotionTotal(Map<String, Object> params);

    @Select("SELECT pp.*, p.product_name, p.price, p.quality " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE promotion_id = #{promotionId} ")
    PromotionBack getPromotionDetailBackById(Long promotionId);


    @Select("SELECT product_id, product_name, price, quality FROM product WHERE product_id = #{productId}")
    ProductInPromotion getProductBack(Long productId);

    @Select("SELECT pp.*, p.product_name, p.price " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE pp.product_id = #{productId} ")
    List<PromotionBack> getPromotionsByProductId(Long productId);


    //新增商品促销信息
    @Select("INSERT INTO productpromotion (product_id, start_time, end_time, promotion_type, discount_rate, reduce_amount, promotion_quantity, per_user_limit, promotion_stock ) " +
            "VALUES (#{product_id}, #{start_time}, #{end_time}, #{promotion_type}, #{discount_rate}, #{reduce_amount}, #{promotion_quantity}, #{per_user_limit}, #{promotion_stock})")
    int insertPromotion(PromotionBack promotion);


    @Update("UPDATE productpromotion SET start_time = #{start_time}, end_time = #{end_time}, promotion_type = #{promotion_type}, discount_rate = #{discount_rate}, reduce_amount = #{reduce_amount}, promotion_quantity = #{promotion_quantity}, per_user_limit = #{per_user_limit}, promotion_stock = #{promotion_stock} WHERE promotion_id = #{promotion_id}")
    int updatePromotion(PromotionBack promotion);

    @Delete("DELETE FROM productpromotion WHERE promotion_id = #{promotionId}")
    int deletePromotion(Long promotionId);


    @Delete("<script>" +
            "DELETE FROM productpromotion " +
            "WHERE promotion_id IN " +
            "<foreach item='item' index='index' collection='promotionIdList' " +
            "open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int deletePromotionMore(@Param("promotionIdList") List<Long> promotionIdList);

    @Select("SELECT stock FROM product WHERE product_id = #{productId}")
    Integer getProductStock(Long productId);
}
