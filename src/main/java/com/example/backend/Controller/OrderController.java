package com.example.backend.Controller;

import com.example.backend.Entity.Order;
import com.example.backend.Entity.OrderItem;
import com.example.backend.Service.OrderService;
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
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Long createOrder(@RequestBody Map<String, Object> params) {
        System.out.println("Received request to create order with params: " + params);
        // 解析参数，创建 Order 和 OrderItem 对象
        // 解析用户 ID
        Integer userId = (Integer) params.get("userId");
        Integer addressId = (Integer) params.get("addressId");

        // 生成订单号
        String orderNo = UUID.randomUUID().toString().replace("-", "");

        // 创建 Order 对象
        Order order = new Order();
        order.setUser_id(userId);
        order.setOrder_no(orderNo);
        order.setCreate_time(new Date());
        order.setUpdate_time(new Date());
        order.setOrder_status("UNPAID"); // 初始订单状态设为待处理
        order.setAddress_id(addressId);

        // 解析订单项列表
        List<Map<String, Object>> orderItemMaps = (List<Map<String, Object>>) params.get("orderItems");
        OrderItem[] orderItems = new OrderItem[orderItemMaps.size()];
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (int i = 0; i < orderItemMaps.size(); i++) {
            Map<String, Object> itemMap = orderItemMaps.get(i);
            OrderItem orderItem = new OrderItem();
            Integer productIdInt = (Integer) itemMap.get("productId");
            orderItem.setProduct_id(productIdInt != null ? Long.valueOf(productIdInt) : null);
            orderItem.setPromotion_id((Integer) itemMap.getOrDefault("promotionId", null));
            orderItem.setQuantity((Integer) itemMap.get("quantity"));
            orderItem.setUnit_price(new BigDecimal(itemMap.get("unitPrice").toString()));
            orderItem.setDiscount_amount(new BigDecimal(itemMap.getOrDefault("discountAmount", "0").toString()));
            orderItem.setTotal_price(orderItem.getUnit_price().multiply(BigDecimal.valueOf(orderItem.getQuantity())));

            // 考虑购买数量，累加每个订单项的总价到总金额
            totalAmount = totalAmount.add(orderItem.getTotal_price());
            discountAmount = discountAmount.add(orderItem.getDiscount_amount());

            orderItems[i] = orderItem;
        }

        order.setTotal_amount(totalAmount);
        order.setDiscount_amount(discountAmount);
        order.setPayment_amount(totalAmount.subtract(discountAmount));
        System.out.println("Order: " + order);

        return orderService.createOrder(order, orderItems);
    }

//    @GetMapping("/pay/{orderId}")
//    public String payOrder(@PathVariable Long orderId, @RequestParam BigDecimal paymentAmount) throws Exception {
//        return orderService.payOrder(orderId, paymentAmount);
//    }

    @GetMapping("/details/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        return orderService.getOrderDetails(orderId);
    }

    @GetMapping("/pay/{orderId}")
    public String payOrder(@PathVariable Long orderId) throws Exception {
        return orderService.payOrder(orderId);
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