package com.example.backend.Model.Vo;

import com.example.backend.Model.Entity.Address;
import com.example.backend.Model.Entity.Express;
import com.example.backend.Model.Entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail {
    private Long order_id;
    private String order_no;
    private String method;
    private Long user_id;
    private BigDecimal total_amount;// 商品原价总和
    private BigDecimal discount_amount; // 总优惠金额
    private BigDecimal goods_amount;// 商品实付金额（优惠后）
    private BigDecimal shipping_fee;// 运费
    private BigDecimal payment_amount; // 订单实付金额（goods_amount + shipping_fee）
    private String order_status;
    private Address address;
    private List<OrderItem> order_items;
    private String remark;
    private Integer express_id;
    private Express express;
    private String express_num;
    private Date create_time;
    private Date pay_time;
}
