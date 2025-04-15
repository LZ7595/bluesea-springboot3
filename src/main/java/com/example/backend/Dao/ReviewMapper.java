package com.example.backend.Dao;

import com.example.backend.Entity.ProductReview;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {
    @Insert("INSERT INTO productreview (order_id, product_id, user_id, rating, review_content, review_time, image_urls) VALUES (#{order_id}, #{product_id}, #{user_id}, #{rating}, #{review_content}, Now(), #{image_urls})")
    int addReview(ProductReview review);

    @Update("UPDATE productreview SET rating = #{rating}, review_content = #{review_content}, image_urls = #{image_urls} WHERE order_id = #{order_id}")
    int updateReview(ProductReview review);

    @Select("SELECT * FROM productreview WHERE order_id = #{orderId}")
    List<ProductReview> getReviewsByOrderId(Integer orderId);

    @Select("SELECT pr.*, u.username, ud.avatar " +
            "FROM productreview pr " +
            "JOIN user u ON pr.user_id = u.id " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "WHERE pr.product_id = #{productId} AND pr.status = true " +
            "ORDER BY pr.review_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<ProductReview> getReviewsByProductId(Integer productId,int offset, int pageSize);

    @Select("SELECT COUNT(*) FROM productreview WHERE product_id = #{productId} AND status = true ")
    int getReviewCountByProductId(Integer productId);
}
