package com.example.backend.Model.Vo;

import com.example.backend.Model.Entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDisplay {
    private Long order_id;
    private BigDecimal total_amount;
    private BigDecimal payment_amount;
    private String order_status;
    private List<String> order_images;
    private List<OrderItem> order_items;
    private Date create_time;
}
