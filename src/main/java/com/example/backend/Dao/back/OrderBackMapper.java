package com.example.backend.Dao.back;

import com.example.backend.Model.Entity.OrderItem;
import com.example.backend.Model.Entity.back.OrderDetailsBack;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper

public interface OrderBackMapper {

    @Select("<script>" +
            "SELECT * FROM `order` " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (order_id = #{searchKeyword} OR user_id = #{searchKeyword}) " +
            "</if> " +
            "</where> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<OrderDetailsBack> SearchOrderList(Map<String, Object> params);

    @Select("<script>" +
            "SELECT COUNT(*) FROM `order` " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (order_id = #{searchKeyword} OR user_id = #{searchKeyword}) " +
            "</if> " +
            "</where> " +
            "</script>")
    int getSearchOrderTotal(Map<String, Object> params);

    @Select("SELECT * FROM `order` WHERE order_id = #{orderId}")
    OrderDetailsBack getOrderDetailsBackById(Long orderId);

    @Select("SELECT * FROM `order_item` WHERE order_id = #{orderId}")
    List<OrderItem> getOrderItemsByOrderId(Long orderId);

    @Update("UPDATE `order` SET order_status = 'SHIPPED', express_num = #{express_num}, express_id = #{express_id} express_time = NOW() WHERE order_id = #{orderId}")
    int ShipOrder(Long orderId, int express_id,String express_num);
}
