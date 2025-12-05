package com.example.backend.Model.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 订单消息实体类，用于RabbitMQ消息传递
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {
    private Long orderId;      // 订单ID
    private Long userId;       // 用户ID
    private String orderNo;    // 订单编号
    private String phone;      // 用户手机号
    private String orderStatus; // 订单状态
}
