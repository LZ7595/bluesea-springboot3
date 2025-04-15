package com.example.backend.Entity.back;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponsePageResultBack {
    private List<OrderDetailsBack> orderResponseList;
    private int total;
}
