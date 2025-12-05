package com.example.backend.Service;

import com.example.backend.Model.Entity.Order;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Long createOrder(Map<String, Object> params);

    ResponseEntity<?> verifyGoodsForUser(Integer userId, List<Map<String, Object>> goodsList);

    String handlePayNotify(String params);

    ResponseEntity<?> getOrderDetails(Long orderId);

    ResponseEntity<?> cancelOrder(Long orderId);

    ResponseEntity<?> getOrdersByUserIdAndStatus(Integer userId, String status, int currentPage, int pageSize);

    ResponseEntity<?> confirmOrder(Long orderId);

    Order getOrderById(Long orderId);

    String createAppPayOrder(Order order, boolean isSandbox) throws Exception;

    String createWebPayOrder(Order order, boolean isSandbox, String clientType) throws Exception;

    Map<String, Object> queryPayStatus(String orderNo, boolean isSandbox) throws Exception;
}