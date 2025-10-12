package com.example.backend.Controller;

import com.example.backend.Entity.Order;
import com.example.backend.Entity.OrderItem;
import com.example.backend.Service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/verifyGoods/{user_id}")
    public ResponseEntity<?> verifyGoods(@PathVariable Integer user_id, @RequestBody List<Map<String, Object>> goodsList) {
        // TODO: 实现验证商品库存的逻辑
        return orderService.verifyGoodsForUser(user_id, goodsList);
    }

    /**
     * 创建订单接口（适配前端确认订单页参数）
     * 前端参数格式：
     * {
     * "user_id": 1110000000,          // 用户ID
     * "address_id": 10,               // 收货地址ID
     * "payment_type": 1,              // 支付方式（1-微信，2-支付宝）
     * "remark": "请尽快发货",          // 订单备注
     * "orderItems": [                 // 订单项列表
     * {
     * "cart_id": 123,             // 购物车ID（可选，用于后续清空购物车）
     * "product_id": 400000026,    // 商品ID
     * "quantity": 1,              // 购买数量
     * "promotion_id": 5,          // 选中的优惠ID（无优惠则为null）
     * "promotion_used_qty": 1,    // 优惠商品数量
     * "original_used_qty": 0,     // 原价商品数量（超出优惠限购的部分）
     * "unit_price": 849.00,       // 优惠后单价
     * "original_price": 999.00    // 商品原价
     * }
     * ]
     * }
     */
    @PostMapping("/create")
    public Long createOrder(@RequestBody Map<String, Object> params) {
        // 1. 日志打印请求参数（便于问题排查）
        log.info("Received create order request, params: {}", params);

        try {

            Long orderId = orderService.createOrder(params);

            // 8. 返回订单ID（前端用于后续支付）
            log.info("Create order success, orderId: {}", orderId);
            return orderId;

        } catch (Exception e) {
            // 异常处理：打印日志并抛出友好异常
            log.error("Create order failed", e);
            throw new RuntimeException("创建订单失败：" + e.getMessage());
        }
    }

    @GetMapping("/details/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        return orderService.getOrderDetails(orderId);
    }

    /**
     * 统一支付接口（支持多端和沙箱环境）
     *
     * @param orderId    订单ID
     * @param isSandbox  是否沙箱环境
     * @param clientType 客户端类型（app/h5/miniprogram）
     */
    @GetMapping("/pay/{orderId}")
    public ResponseEntity<?> payOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false, defaultValue = "false") boolean isSandbox,
            @RequestHeader("Client-Type") String clientType) throws Exception {// 1. 获取订单信息
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. 根据客户端类型处理支付
        if ("app".equals(clientType)) {
            // App端：返回签名后的订单字符串
            String orderString = orderService.createAppPayOrder(order, isSandbox);
            return ResponseEntity.ok(Map.of("orderString", orderString));
        } else {
            // H5/小程序：返回支付表单HTML
            String formHtml = orderService.createWebPayOrder(order, isSandbox, clientType);
            return ResponseEntity.ok(Map.of("form", formHtml));
        }
    }

    /**
     * 支付结果查询接口（供前端轮询使用）
     */
    @GetMapping("/pay/query/{orderNo}")
    public ResponseEntity<?> queryPayResult(
            @PathVariable String orderNo,
            @RequestParam(required = false, defaultValue = "false") boolean isSandbox) {
        try {
            Map<String, Object> result = orderService.queryPayStatus(orderNo, isSandbox);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/pay/notify")
    public String handlePayNotify(@RequestBody String params) {
        return orderService.handlePayNotify(params);
    }

    @GetMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) throws Exception {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUserIdAndStatus(@PathVariable Integer userId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int currentPage,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        return orderService.getOrdersByUserIdAndStatus(userId, status, currentPage, pageSize);
    }

    @GetMapping("/confirm/{orderId}")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId) throws Exception {
        return orderService.confirmOrder(orderId);
    }
}