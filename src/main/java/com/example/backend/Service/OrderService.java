package com.example.backend.Service;

import com.example.backend.Entity.Order;
import com.example.backend.Entity.OrderItem;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

public interface OrderService {
    Long createOrder(Order order, OrderItem[] orderItems);

    //    String payOrder(Long orderId, BigDecimal paymentAmount) throws Exception;
    String payOrder(Long orderId) throws Exception;

    String handlePayNotify(String params);

    ResponseEntity<?> getOrderDetails(Long orderId);

    ResponseEntity<?> cancelOrder(Long orderId);

    ResponseEntity<?> getOrdersByUserIdAndStatus(Integer userId, String status, int currentPage, int pageSize);

    ResponseEntity<?> confirmOrder(Long orderId);
}