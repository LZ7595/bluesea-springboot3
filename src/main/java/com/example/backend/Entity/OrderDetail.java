package com.example.backend.Entity;

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
    private Long user_id;
    private BigDecimal total_amount;
    private BigDecimal discount_amount;
    private BigDecimal payment_amount;
    private String order_status;
    private Address address;
    private List<OrderItem> order_items;
    private Date create_time;
    private Date pay_time;
}
