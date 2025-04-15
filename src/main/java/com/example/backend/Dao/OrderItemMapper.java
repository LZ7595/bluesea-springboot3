package com.example.backend.Dao;

import com.example.backend.Entity.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper {
    @Insert("INSERT INTO order_item (order_id, product_id, promotion_id, quantity, unit_price, discount_amount, total_price) " +
            "VALUES (#{order_id}, #{product_id}, #{promotion_id}, #{quantity}, #{unit_price}, #{discount_amount}, #{total_price})")
    @Options(useGeneratedKeys = true, keyProperty = "item_id")
    void insertOrderItem(OrderItem orderItem);

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> getOrderItemsByOrderId(Long orderId);
}