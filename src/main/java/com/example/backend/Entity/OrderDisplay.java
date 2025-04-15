package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
}
