package com.example.backend.Model.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private Long order_id;                // 订单ID（自增主键）
    private Long user_id;           // 用户ID
    private String order_no;        // 订单号
    private String method;          // 支付方式文本（WECHAT/ALIPAY/OTHER）
    private Integer payment_type;   // 支付方式数字（1-微信，2-支付宝）
    private BigDecimal total_amount;// 商品原价总和
    private BigDecimal discount_amount; // 总优惠金额
    private BigDecimal goods_amount;// 商品实付金额（优惠后）
    private BigDecimal shipping_fee;// 运费
    private BigDecimal payment_amount; // 订单实付金额（goods_amount + shipping_fee）
    private String order_status;    // 订单状态（UNPAID/PAID/SHIPPED/COMPLETED/CANCELED）
    private Long address_id;        // 收货地址ID
    private String remark;          // 订单备注
    private Date create_time;       // 创建时间
    private Date update_time;       // 更新时间
}
