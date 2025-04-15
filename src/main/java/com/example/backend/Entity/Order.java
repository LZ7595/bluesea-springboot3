package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private Long order_id;
    private Integer user_id;
    private String order_no;
    private BigDecimal total_amount;
    private BigDecimal discount_amount;
    private BigDecimal payment_amount;
    private String order_status;
    private Integer address_id;
    private Date create_time;
    private Date update_time;
}
