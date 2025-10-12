package com.example.backend.Dao;

import com.example.backend.Entity.Address;
import com.example.backend.Entity.OrderDetail;
import com.example.backend.Entity.Order;
import com.example.backend.Entity.OrderDisplay;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单记录（不含payment_type字段）
     * @param order 订单实体
     */
    @Insert("INSERT INTO `order` (" +
            "user_id, order_no, method, total_amount, discount_amount, " +
            "goods_amount, shipping_fee, payment_amount, order_status, " +
            "address_id, remark, create_time, update_time" +
            ") VALUES (" +
            "#{user_id}, #{order_no}, #{method}, #{total_amount}, #{discount_amount}, " +
            "#{goods_amount}, #{shipping_fee}, #{payment_amount}, #{order_status}, " +
            "#{address_id}, #{remark}, #{create_time}, #{update_time}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "order_id")
    void insertOrder(Order order);

    @Select("SELECT * FROM `order` WHERE order_id = #{orderId}")
    Order getOrderById(Long orderId);
    @Select("SELECT * FROM `order` WHERE order_id = #{orderId}")
    OrderDetail getOrderDetail(Long orderId);

    @Update("UPDATE `order` SET order_status = 'PAID', pay_time = NOW() WHERE order_no = #{order_no}")
    int updateOrderStatus(String order_no);

    @Update("UPDATE `order` SET order_status = 'CANCELED' WHERE order_id = #{orderId}")
    int cancelOrder(Long orderId);

    @Select("<script>" +
            "SELECT * FROM `order` " +
            "WHERE user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND order_status = #{status} " +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{pageSize} " +
            "</script>")
    List<OrderDisplay> getOrdersByUserIdAndStatus(Integer userId, String status,int offset, int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM `order` " +
            "WHERE user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND order_status = #{status} " +
            "</if>" +
            "</script>")
    int getOrdersByUserIdAndStatusCount(Integer userId, String status);

    @Update("UPDATE `order` SET order_status = 'COMPLETED', confirm_time = NOW() WHERE order_id = #{orderId}")
    int confirmOrder(Long orderId);
}