package com.example.backend.Model.Entity;

import com.example.backend.Model.Vo.ProductPayInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long item_id;                // 订单项ID（自增主键）
    private Long order_id;          // 关联订单ID
    private Long cart_id;           // 关联购物车ID（可选）
    private Long product_id;        // 商品ID
    private ProductPayInfo product;  // 商品信息
    private Integer promotion_id;      // 关联优惠ID（可选）
    private Integer quantity;       // 购买总数量
    private BigDecimal original_price; // 商品原价
    private BigDecimal unit_price;  // 优惠后单价
    private BigDecimal discount_amount; // 订单项优惠金额
    private BigDecimal total_price; // 订单项总价（优惠后）
}
