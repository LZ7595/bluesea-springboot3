package com.example.backend.Impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.example.backend.Entity.OrderDetail;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.example.backend.Dao.*;
import com.example.backend.Entity.*;
import com.example.backend.Service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductImageMapper productImageMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductPromotionMapper productPromotionMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Value("${alipay.appId}")
    private String appId;
    @Value("${alipay.privateKey}")
    private String privateKey;
    @Value("${alipay.alipayPublicKey}")
    private String alipayPublicKey;
    @Value("${alipay.gatewayUrl}")
    private String gatewayUrl;
    @Value("${alipay.returnUrl}")
    private String returnUrl;
    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    @Override
    @Transactional
    public Long createOrder(Order order, OrderItem[] orderItems) {
        System.out.println(order);
        System.out.println(orderItems);
        orderMapper.insertOrder(order);
        Long orderId = order.getOrder_id();
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder_id(orderId);
            orderItemMapper.insertOrderItem(orderItem);
            Long productId = orderItem.getProduct_id();
            Integer quantity = orderItem.getQuantity();
            Product product = productMapper.getProductById(productId);
            if (product != null) {
                Integer currentStock = product.getStock();
                Integer currentSales_volume = product.getSales_volume();
                if (currentStock >= quantity) {
                    product.setStock(currentStock - quantity);
                    product.setSales_volume(currentSales_volume + quantity);
                    productMapper.updateProductStockAndSales(product);
                } else {
                    // 库存不足，处理逻辑可以根据需求进行调整，比如抛出异常
                    throw new RuntimeException("库存不足，无法完成订单");
                }
            }
        }
        return orderId;
    }

//    @Override
//    public String payOrder(Long orderId, BigDecimal paymentAmount) throws Exception {
//        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl, appId, privateKey, "json", "UTF-8", alipayPublicKey, "RSA2");
//        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
//        alipayRequest.setReturnUrl(returnUrl);
//        alipayRequest.setNotifyUrl(notifyUrl);
//
//        JSONObject bizContent = new JSONObject();
//        bizContent.put("out_trade_no", orderId);
//        bizContent.put("total_amount", paymentAmount);
//        bizContent.put("subject", "订单支付");
//        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
//        alipayRequest.setBizContent(bizContent.toString());
//
//        AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
//        if (response.isSuccess()) {
//            return response.getBody();
//        } else {
//            throw new Exception("支付宝支付请求失败");
//        }
//    }

    @Override
    public ResponseEntity<?> getOrderDetails(Long orderId) {
        try {
            OrderDetail orderDetail = orderMapper.getOrderDetail(orderId);
            if (orderDetail != null) {
                Address address = addressMapper.getAddressByOrderId(orderId);
                orderDetail.setAddress(address);
                List<OrderItem> orderItems = orderItemMapper.getOrderItemsByOrderId(orderId);
                List<OrderItem> orderItemDetails = orderItems.stream().map(item -> {
                    item.setProduct(getProductPayInfo(item.getProduct_id(), item.getPromotion_id()));
                    return item;
                }).collect(Collectors.toList());
                orderDetail.setOrder_items(orderItemDetails);
                return ResponseEntity.ok(orderDetail);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("订单不存在");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取订单详情失败");
        }
    }

    public ProductPayInfo getProductPayInfo(Long productId, Integer promotionId) {
        ProductPayInfo productDetails = productMapper.getProductDetails(productId);
        ProductPromotion promotion = productPromotionMapper.getProductPromotionById(promotionId);
        List<ProductPromotion> promotions = new ArrayList<>();
        promotions.add(promotion);
        productDetails.setPromotions(promotions);
        ProductImage main_img = productImageMapper.getProductMainImageByProductId(productId);
        productDetails.setProduct_main_image(main_img.getImage_url());
        return productDetails;
    }

    @Override
    public String payOrder(Long orderId) throws Exception {
        System.out.println(orderId);
        Order order = orderMapper.getOrderById(orderId);
        System.out.println(order);
        if (order == null) {
            throw new Exception("订单不存在");
        }

        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl, appId, privateKey, "json", "UTF-8", alipayPublicKey, "RSA2");
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(returnUrl + orderId);
        alipayRequest.setNotifyUrl(notifyUrl);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrder_no());
        bizContent.put("total_amount", order.getPayment_amount());
        bizContent.put("subject", "订单支付");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        alipayRequest.setBizContent(bizContent.toString());

        AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
        if (response.isSuccess()) {
            return response.getBody();
        } else {
            throw new Exception("支付宝支付请求失败");
        }
    }
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public String handlePayNotify(String params) {
        try {
            // 解析支付宝回调参数
            logger.info("收到支付宝支付回调，参数：{}", params);
            Map<String, String> paramsMap = parseParams(params);
            logger.info("解析后的支付宝支付回调参数：{}", paramsMap);

            // 验证签名
            boolean signVerified = verifySign(paramsMap);
            if (!signVerified) {
                logger.error("支付宝支付回调签名验证失败，参数：{}", paramsMap);
                return "fail";
            }

            // 检查交易状态
            String tradeStatus = paramsMap.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                logger.error("支付宝支付回调交易状态不是成功，状态：{}", tradeStatus);
                return "fail";
            }

            // 处理订单状态更新等逻辑
            String outTradeNo = paramsMap.get("out_trade_no");
            if (outTradeNo == null) {
                logger.error("支付宝支付回调中缺少 out_trade_no 参数，参数：{}", paramsMap);
                return "fail";
            }

            // 示例：更新订单状态为已支付
            int updateResult = orderMapper.updateOrderStatus(outTradeNo);
            if (updateResult > 0) {
                logger.info("订单 {} 状态更新为已支付成功", outTradeNo);
                return "success";
            } else {
                logger.error("订单 {} 状态更新为已支付失败", outTradeNo);
                return "fail";
            }
        } catch (Exception e) {
            logger.error("处理支付宝支付回调时发生异常", e);
            return "fail";
        }
    }

    public ResponseEntity<?> cancelOrder(Long orderId){
        try{
            int result = orderMapper.cancelOrder(orderId);
            if (result > 0) {
                return ResponseEntity.ok("订单取消成功");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("订单取消失败");
            }
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    public ResponseEntity<?> getOrdersByUserIdAndStatus(Integer userId, String status, int currentPage, int pageSize){
        try {
            int offset = (currentPage - 1) * pageSize;
            List<OrderDisplay> orders = orderMapper.getOrdersByUserIdAndStatus(userId, status, offset, pageSize);
            int total = orderMapper.getOrdersByUserIdAndStatusCount(userId, status);
            if (orders != null) {
                List<OrderDisplay> orderDisplayList = orders.stream().map(order -> {
                    List<OrderItem> orderItems = orderItemMapper.getOrderItemsByOrderId(order.getOrder_id());
                    List<String> imgUrls = new ArrayList<>();
                    List<OrderItem> orderItemDetails = orderItems.stream().map(item -> {
                        item.setProduct(getProductPayInfo(item.getProduct_id(), item.getPromotion_id()));
                        imgUrls.add(item.getProduct().getProduct_main_image());
                        return item;
                    }).collect(Collectors.toList());
                    order.setOrder_items(orderItemDetails);
                    order.setOrder_images(imgUrls);
                    return order;
                }).collect(Collectors.toList());
                return ResponseEntity.ok(new OrderDisplayPageResult(orderDisplayList, total));
            }else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未找到订单");
            }
        }catch (Exception e){
            e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    /**
     * 验证支付宝回调签名
     * @param paramsMap 回调参数
     * @return 签名验证结果
     */
    private boolean verifySign(Map<String, String> paramsMap) {
        try {
            // 调用支付宝 SDK 的签名验证方法
            boolean result = AlipaySignature.rsaCheckV1(
                    paramsMap,
                    alipayPublicKey,
                    "UTF-8",
                    "RSA2"
            );
            return result;
        } catch (AlipayApiException e) {
            // 签名验证过程中出现异常，记录错误日志
            System.err.println("支付宝签名验证异常: " + e.getMessage());
            return false;
        }
    }

    public ResponseEntity<?> confirmOrder(Long orderId) {
        try {
            int result = orderMapper.confirmOrder(orderId);
            if (result > 0) {
                return ResponseEntity.ok("订单确认成功");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("订单确认失败");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    /**
     * 解析 URL 编码的参数
     * @param params URL 编码的参数
     * @return 解析后的参数 Map
     * @throws UnsupportedEncodingException 编码异常
     */
    private Map<String, String> parseParams(String params) throws UnsupportedEncodingException {
        Map<String, String> paramsMap = new HashMap<>();
        String[] pairs = params.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
            String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
            paramsMap.put(key, value);
        }
        return paramsMap;
    }
}