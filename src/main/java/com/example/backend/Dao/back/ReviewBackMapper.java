package com.example.backend.Dao.back;

import com.example.backend.Entity.Product;
import com.example.backend.Entity.ProductReview;
import com.example.backend.Entity.back.ProductDetailsBack;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewBackMapper {
    @Select("<script>" +
            "SELECT pr.*, u.username, ud.avatar, p.product_name " +
            "FROM productreview pr " +
            "JOIN user u ON pr.user_id = u.id " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "JOIN product p ON pr.product_id = p.product_id " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "(p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR u.id LIKE CONCAT('%', #{searchKeyword}, '%') OR pr.review_content LIKE CONCAT('%', #{searchKeyword}, '%') OR pr.review_id LIKE CONCAT('%', #{searchKeyword}, '%') OR u.username LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize} " +
            "</script>")
    List<ProductReview> SearchReviewList(Map<String, Object> params);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM productreview pr " +
            "JOIN user u ON pr.user_id = u.id " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "(p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR u.id LIKE CONCAT('%', #{searchKeyword}, '%') OR pr.review_content LIKE CONCAT('%', #{searchKeyword}, '%') OR pr.review_id LIKE CONCAT('%', #{searchKeyword}, '%') OR u.username LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "</script>")
    int getSearchReviewTotal(Map<String, Object> params);

    @Select("SELECT pr.*, u.username, ud.avatar " +
            "FROM productreview pr " +
            "JOIN user u ON pr.user_id = u.id " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "WHERE pr.review_id = #{reviewId} ")
    ProductReview getReviewDetailsBackById(Long reviewId);

    @Delete("DELETE FROM productreview WHERE review_id = #{reviewId}")
    int deleteReview(Long reviewId);


    @Delete("<script>" +
            "DELETE FROM productreview " +
            "WHERE review_id IN " +
            "<foreach item='item' index='index' collection='reviewIdList' " +
            "open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int deleteReviewMore(@Param("reviewIdList") List<Long> reviewIdList);

    @Update("UPDATE productreview SET status = #{status} WHERE review_id = #{reviewId}")
    int changeStatus(Integer reviewId, boolean status);
}
