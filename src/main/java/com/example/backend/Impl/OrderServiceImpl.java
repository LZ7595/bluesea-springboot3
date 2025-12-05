package com.example.backend.Impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.example.backend.Config.AlipayConfig;
import com.example.backend.Config.RabbitConfig;
import com.example.backend.Dao.*;
import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Dto.ProductPromotionWrapper;
import com.example.backend.Model.Dto.PromotionUsedCountDTO;
import com.example.backend.Model.Entity.*;
import com.example.backend.Model.Message.OrderMessage;
import com.example.backend.Model.Vo.OrderDetail;
import com.example.backend.Model.Vo.OrderDisplay;
import com.example.backend.Model.Vo.ProductPayInfo;
import com.example.backend.Service.OrderService;
import com.example.backend.Utils.PromotionUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.backend.Utils.RedisConstants.PRODUCT_DETAILS_KEY;
import static com.example.backend.Utils.RedisConstants.PRODUCT_PROMOTIONS_KEY;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Value("${alipay.sandbox.publicKey}")
    private String alipayPublicKey;
    @Autowired
    private AlipayConfig alipayConfig;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductImageMapper productImageMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShoppingCartMapper cartMapper;
    @Autowired
    private ProductPromotionMapper productPromotionMapper;
    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private ExpressMapper expressMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    public Order getOrderById(Long orderId) {
        return orderMapper.getOrderById(orderId);
    }

    public ResponseEntity<?> verifyGoodsForUser(Integer userId, List<Map<String, Object>> goodsList) {
        log.info("verifyGoodsForUser: userId={}, goodsList={}", userId, goodsList);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "请先登录"));
        }

        List<Long> productIds = goodsList.stream()
                .filter(goods -> goods.get("product_id") != null)
                .map(goods -> {
                    Object productIdObj = goods.get("product_id");
                    if (productIdObj instanceof Number) {
                        return ((Number) productIdObj).longValue();
                    } else if (productIdObj instanceof String) {
                        return Long.parseLong((String) productIdObj);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Product> productMap = productMapper.getProductBaseInfoBatch(productIds).stream()
                .collect(Collectors.toMap(Product::getProduct_id, p -> p));

        List<ProductPromotion> allPromotions = productMapper.getAvailablePromotionsBatch(userId, productIds);

        Map<Long, Integer> usedCountMap = new HashMap<>();
        List<PromotionUsedCountDTO> usedCountList = productMapper.getPromotionUsedCountByUser(userId, productIds);
        if (usedCountList != null && !usedCountList.isEmpty()) {
            usedCountMap = usedCountList.stream()
                    .filter(dto -> dto.getPromotionId() != null)
                    .collect(Collectors.toMap(
                            PromotionUsedCountDTO::getPromotionId,
                            PromotionUsedCountDTO::getUsedCount,
                            (existing, replacement) -> existing
                    ));
        }

        Map<Long, Map<String, Object>> productResultMap = new HashMap<>();
        for (Long productId : productIds) {
            Product product = productMap.get(productId);
            if (product == null) continue;

            List<ProductPromotion> productPromotions = allPromotions.stream()
                    .filter(p -> p.getProduct_id().equals(productId))
                    .collect(Collectors.toList());

            ProductPromotionWrapper promotionWrapper = new ProductPromotionWrapper();
            PromotionUtil.splitUsableAndUnusablePromotions(
                    productPromotions,
                    usedCountMap,
                    product.getStock(),
                    promotionWrapper
            );

            BigDecimal bestPrice = promotionWrapper.getUsablePromotions().stream()
                    .map(ProductPromotion::getDiscount_price)
                    .min(BigDecimal::compareTo)
                    .orElse(product.getPrice());

            Map<String, Object> productResult = new HashMap<>();
            productResult.put("product_id", productId);
            productResult.put("original_price", product.getPrice());
            productResult.put("latest_price", bestPrice);
            productResult.put("product_stock", product.getStock());
            productResult.put("promotion_wrapper", promotionWrapper);
            productResult.put("has_usable_promotion", !promotionWrapper.getUsablePromotions().isEmpty());

            productResultMap.put(productId, productResult);
        }

        return ResponseEntity.ok().body(productResultMap);
    }


    @Override
    @Transactional
    public Long createOrder(Map<String, Object> params) {

        // 解析基础参数
        Long userId = ((Number) params.get("user_id")).longValue();
        Long addressId = ((Number) params.get("address_id")).longValue();
        String remark = params.getOrDefault("remark", "").toString();

        // 生成订单号
        String orderNo = UUID.randomUUID().toString().replace("-", "");

        // 构建Order实体
        Order order = new Order();
        order.setUser_id(userId);
        order.setAddress_id(addressId);
        order.setOrder_no(orderNo);
        order.setRemark(remark);
        order.setCreate_time(new Date());
        order.setUpdate_time(new Date());
        order.setOrder_status("UNPAID");

        // 解析订单项列表
        List<Map<String, Object>> orderItemMaps = (List<Map<String, Object>>) params.get("orderItems");
        if (orderItemMaps == null || orderItemMaps.isEmpty()) {
            log.error("Create order failed: orderItems is empty");
            throw new IllegalArgumentException("订单项不能为空");
        }

        OrderItem[] orderItems = new OrderItem[orderItemMaps.size()];
        BigDecimal totalAmount = BigDecimal.ZERO;       // 商品原价总和
        BigDecimal discountAmount = BigDecimal.ZERO;    // 总优惠金额
        BigDecimal actualGoodsAmount = BigDecimal.ZERO; // 商品实付金额（优惠后）

        List<Long> cartIdsToDelete = new ArrayList<>();
        // 新增：用于记录需要清除缓存的商品ID
        List<Long> productIdsForCacheClear = new ArrayList<>();

        int promotionUsedQty = 0;

        // 遍历解析每个订单项
        for (int i = 0; i < orderItemMaps.size(); i++) {
            Map<String, Object> itemMap = orderItemMaps.get(i);
            OrderItem orderItem = new OrderItem();

            // 订单项基础信息
            Long productId = ((Number) itemMap.get("product_id")).longValue();
            // 新增：记录商品ID用于后续清除缓存
            productIdsForCacheClear.add(productId);

            Integer quantity = ((Number) itemMap.get("quantity")).intValue();
            Integer promotionId = itemMap.get("promotion_id") != null ?
                    ((Number) itemMap.get("promotion_id")).intValue() : null;

            Long cartId = itemMap.get("cart_id") != null ?
                    ((Number) itemMap.get("cart_id")).longValue() : null;
            if (cartId != null && cartId > 0) {
                cartIdsToDelete.add(cartId);
            }

            orderItem.setProduct_id(productId);
            orderItem.setQuantity(quantity);
            orderItem.setPromotion_id(promotionId);

            // 解析优惠数量和原价数量
            promotionUsedQty = ((Number) itemMap.get("promotion_used_qty")).intValue();
            int originalUsedQty = ((Number) itemMap.get("original_used_qty")).intValue();

            // ===== 优惠规则校验 =====
            // 1. 无优惠时，优惠数量必须为0
            if (promotionId == null) {
                if (promotionUsedQty > 0) {
                    log.error("商品{}: 未选择优惠但优惠数量不为0", productId);
                    throw new IllegalArgumentException("商品" + productId + "未选择优惠，不可使用优惠数量");
                }
            }
            // 2. 有优惠时，校验优惠合法性
            else {
                ProductPromotion promotion = productPromotionMapper.getProductPromotionById(promotionId);
                if (promotion == null) {
                    log.error("商品{}: 优惠ID{}不存在", productId, promotionId);
                    throw new IllegalArgumentException("商品" + productId + "选择的优惠已失效");
                }

                // 校验优惠有效期
                Date now = new Date();
                if (promotion.getStart_time().after(now) || promotion.getEnd_time().before(now)) {
                    log.error("商品{}: 优惠ID{}已过期或未开始", productId, promotionId);
                    throw new IllegalArgumentException("商品" + productId + "选择的优惠已过期");
                }

                // 校验优惠限购数量
                if (promotionUsedQty > promotion.getPer_user_limit()) {
                    log.error("商品{}: 优惠ID{}限购{}件，实际使用{}件",
                            productId, promotionId, promotion.getPer_user_limit(), promotionUsedQty);
                    throw new IllegalArgumentException("商品" + productId + "优惠限购" + promotion.getPer_user_limit() + "件");
                }

                // 校验优惠库存
                if (promotionUsedQty > promotion.getPromotion_quantity()) {
                    log.error("商品{}: 优惠ID{}库存{}件，实际使用{}件",
                            productId, promotionId, promotion.getPromotion_quantity(), promotionUsedQty);
                    throw new IllegalArgumentException("商品" + productId + "优惠库存不足，仅剩" + promotion.getPromotion_quantity() + "件");
                }
            }

            // 3. 校验数量总和一致性
            if (promotionUsedQty + originalUsedQty != quantity) {
                log.error("商品{}: 数量拆分异常（优惠{}+原价{}≠总数量{}）",
                        productId, promotionUsedQty, originalUsedQty, quantity);
                throw new IllegalArgumentException("商品" + productId + "数量拆分异常，请重新选择");
            }

            // ===== 价格计算 =====
            BigDecimal originalPrice;
            BigDecimal unitPrice;
            try {
                // 解析原价并强制保留2位小数
                originalPrice = new BigDecimal(itemMap.get("original_price").toString())
                        .setScale(2, RoundingMode.HALF_UP);

                // 解析优惠价（无优惠时与原价一致）
                if (promotionId == null) {
                    unitPrice = originalPrice;
                } else {
                    unitPrice = new BigDecimal(itemMap.get("unit_price").toString())
                            .setScale(2, RoundingMode.HALF_UP);
                }

                // 校验优惠价不能大于原价
                if (unitPrice.compareTo(originalPrice) > 0) {
                    log.error("商品{}: 优惠价{} > 原价{}", productId, unitPrice, originalPrice);
                    throw new IllegalArgumentException("商品" + productId + "价格异常");
                }
            } catch (NumberFormatException e) {
                log.error("商品{}: 价格格式错误（原价:{}，优惠价:{}）",
                        productId, itemMap.get("original_price"), itemMap.get("unit_price"));
                throw new IllegalArgumentException("商品" + productId + "价格格式错误");
            }

            // 计算订单项金额
            BigDecimal promotionAmount = unitPrice.multiply(BigDecimal.valueOf(promotionUsedQty)); // 优惠部分金额
            BigDecimal originalAmount = originalPrice.multiply(BigDecimal.valueOf(originalUsedQty)); // 原价部分金额
            BigDecimal itemTotalPrice = promotionAmount.add(originalAmount).setScale(2, RoundingMode.HALF_UP); // 订单项总价

            // 计算订单项优惠金额
            BigDecimal itemDiscountAmount = originalPrice.subtract(unitPrice)
                    .multiply(BigDecimal.valueOf(promotionUsedQty))
                    .setScale(2, RoundingMode.HALF_UP);

            // 打印优惠计算日志
            log.debug("商品{}优惠计算: 原价={}, 优惠价={}, 优惠数量={}, 优惠金额={}",
                    productId, originalPrice, unitPrice, promotionUsedQty, itemDiscountAmount);

            // 赋值订单项
            orderItem.setUnit_price(unitPrice);
            orderItem.setOriginal_price(originalPrice);
            orderItem.setTotal_price(itemTotalPrice);
            orderItem.setDiscount_amount(itemDiscountAmount);

            // 累加订单总金额
            totalAmount = totalAmount.add(originalPrice.multiply(BigDecimal.valueOf(quantity)));
            discountAmount = discountAmount.add(itemDiscountAmount);
            actualGoodsAmount = actualGoodsAmount.add(itemTotalPrice);

            orderItems[i] = orderItem;
        }

        // 计算运费及最终金额
        BigDecimal shippingFee = new BigDecimal("10.00");
        BigDecimal freeShippingThreshold = new BigDecimal("1.00");
        BigDecimal finalShippingFee = actualGoodsAmount.compareTo(freeShippingThreshold) >= 0 ?
                BigDecimal.ZERO : shippingFee;

        // 赋值订单金额信息
        order.setTotal_amount(totalAmount);
        order.setDiscount_amount(discountAmount);
        order.setGoods_amount(actualGoodsAmount);
        order.setShipping_fee(finalShippingFee);
        order.setPayment_amount(actualGoodsAmount.add(finalShippingFee));

        log.info("创建订单: {}", order);

        // 保存订单及订单项
        orderMapper.insertOrder(order);
        Long orderId = order.getOrder_id();

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder_id(orderId);
            orderItemMapper.insertOrderItem(orderItem);

            // 扣减商品库存和优惠库存
            Product product = productMapper.getProductById(orderItem.getProduct_id());
            if (product != null) {
                Integer currentStock = product.getStock();
                if (currentStock < orderItem.getQuantity()) {
                    throw new RuntimeException("商品" + orderItem.getProduct_id() + "库存不足");
                }
                // 更新商品库存和销量
                product.setStock(currentStock - orderItem.getQuantity());
                product.setSales_volume(product.getSales_volume() + orderItem.getQuantity());
                productMapper.updateProductStockAndSales(product);

                // 扣减优惠库存（如有）
                if (orderItem.getPromotion_id() != null && promotionUsedQty > 0) {
                    ProductPromotion promotion = productPromotionMapper.getProductPromotionById(orderItem.getPromotion_id());
                    if (promotion != null) {
                        promotion.setPromotion_quantity(promotion.getPromotion_quantity() - promotionUsedQty);
                        productPromotionMapper.updatePromotionStock(promotion);
                    }
                }
            }
        }

        // 新增：清除相关缓存
        for (Long productId : productIdsForCacheClear) {
            // 1. 清除商品详情缓存
            String productDetailsKey = PRODUCT_DETAILS_KEY + productId;
            stringRedisTemplate.delete(productDetailsKey);
            log.info("订单创建成功，清除商品详情缓存: {}", productDetailsKey);

            // 2. 清除当前用户的商品优惠缓存
            String userPromotionKey = PRODUCT_PROMOTIONS_KEY + productId + ":user:" + userId;
            stringRedisTemplate.delete(userPromotionKey);
            log.info("订单创建成功，清除用户商品优惠缓存: {}", userPromotionKey);

            // 3. 清除匿名用户的商品优惠缓存
            String anonymousPromotionKey = PRODUCT_PROMOTIONS_KEY + productId + ":anonymous";
            stringRedisTemplate.delete(anonymousPromotionKey);
            log.info("订单创建成功，清除匿名用户商品优惠缓存: {}", anonymousPromotionKey);
        }

        if (!cartIdsToDelete.isEmpty()) {
            try {
                int deleteCount = cartMapper.batchDeleteCartItems(cartIdsToDelete, userId);
                log.info("订单创建成功，删除购物车项{}个，购物车ID列表：{}", deleteCount, cartIdsToDelete);
            } catch (Exception e) {
                // 记录警告日志但不影响订单创建（非核心流程）
                log.warn("删除购物车项失败，购物车ID列表：{}", cartIdsToDelete, e);
            }
        }

        // 获取用户地址信息（用于获取手机号）
        Address address = addressMapper.selectAddressById(addressId.intValue());
        String phone = address != null ? address.getPhone() : "";

        // 构建订单消息
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOrderId(orderId);
        orderMessage.setUserId(userId);
        orderMessage.setOrderNo(orderNo);
        orderMessage.setPhone(phone);
        orderMessage.setOrderStatus(order.getOrder_status());

        // 1. 发送即时消息：订单创建通知（用于日志记录等）
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_NOTIFY_EXCHANGE,
                RabbitConfig.ORDER_NOTIFY_ROUTING_KEY,
                orderMessage
        );
        log.info("订单创建成功，已发送通知消息: orderId={}", orderId);

        // 2. 发送延迟消息：30分钟后检查订单是否支付
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_DELAY_EXCHANGE,
                RabbitConfig.ORDER_CANCEL_ROUTING_KEY,
                orderMessage,
                message -> {
                    // 设置延迟时间：30分钟（单位：毫秒）
                    message.getMessageProperties().setHeader("x-delay", 30 * 60 * 1000);
                    return message;
                }
        );
        log.info("订单创建成功，已发送延迟取消消息: orderId={}", orderId);
        // ============================================================

        return orderId;
    }


    @Override
    public ResponseEntity<?> getOrderDetails(Long orderId) {
        try {
            OrderDetail orderDetail = orderMapper.getOrderDetail(orderId);
            if (orderDetail != null) {
                Address address = addressMapper.getAddressByOrderId(orderId);
                orderDetail.setAddress(address);
                if (orderDetail.getExpress_id() != null) {
                    Express express = expressMapper.getExpressById(orderDetail.getExpress_id());
                    orderDetail.setExpress(express);
                }
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
            e.printStackTrace();
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
    public String createAppPayOrder(Order order, boolean isSandbox) throws Exception {
        AlipayClient alipayClient = alipayConfig.getAlipayClient(isSandbox);
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl(isSandbox));

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrder_no());
        bizContent.put("total_amount", order.getPayment_amount().toString());
        bizContent.put("subject", "订单支付-" + order.getOrder_no());
        bizContent.put("product_code", "QUICK_MSECURITY_PAY");
        request.setBizContent(bizContent.toString());

        AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
        if (response.isSuccess()) {
            String orderString = response.getBody();
            log.info("生成的支付串: {}", orderString.replaceAll("sign=[^&]+", "sign=***"));
            return orderString;
        } else {
            throw new Exception("创建App支付订单失败: " + response.getMsg());
        }
    }

    @Override
    public String createWebPayOrder(Order order, boolean isSandbox, String clientType) throws Exception {
        AlipayClient alipayClient = alipayConfig.getAlipayClient(isSandbox);
        String productCode;

        if ("miniprogram".equals(clientType) || "h5-mobile".equals(clientType)) {
            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            productCode = "QUICK_WAP_WAY";
            String returnUrl = alipayConfig.getReturnUrl(isSandbox) + "?orderId=" + order.getOrder_id();
            request.setReturnUrl(returnUrl);
            request.setNotifyUrl(alipayConfig.getNotifyUrl(isSandbox));

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", order.getOrder_no());
            bizContent.put("total_amount", order.getPayment_amount().toString());
            bizContent.put("subject", "订单支付-" + order.getOrder_no());
            bizContent.put("product_code", productCode);

            if ("miniprogram".equals(clientType)) {
                bizContent.put("quit_url", returnUrl + "&from=mini");
            }

            request.setBizContent(bizContent.toString());
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                return response.getBody();
            } else {
                throw new Exception("创建手机网站支付订单失败: " + response.getMsg() + "，错误代码: " + response.getCode());
            }
        } else {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            productCode = "FAST_INSTANT_TRADE_PAY";
            String returnUrl = alipayConfig.getReturnUrl(isSandbox) + "?orderId=" + order.getOrder_id();
            request.setReturnUrl(returnUrl);
            request.setNotifyUrl(alipayConfig.getNotifyUrl(isSandbox));

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", order.getOrder_no());
            bizContent.put("total_amount", order.getPayment_amount().toString());
            bizContent.put("subject", "订单支付-" + order.getOrder_no());
            bizContent.put("product_code", productCode);

            request.setBizContent(bizContent.toString());
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                return response.getBody();
            } else {
                throw new Exception("创建电脑网站支付订单失败: " + response.getMsg() + "，错误代码: " + response.getCode());
            }
        }
    }


    @Override
    public Map<String, Object> queryPayStatus(String orderNo, boolean isSandbox) throws Exception {
        AlipayClient alipayClient = alipayConfig.getAlipayClient(isSandbox);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toString());

        AlipayTradeQueryResponse response = alipayClient.execute(request);
        Map<String, Object> result = new HashMap<>();

        if (response.isSuccess()) {
            result.put("success", true);
            result.put("tradeStatus", response.getTradeStatus());
            result.put("tradeNo", response.getTradeNo());
        } else {
            result.put("success", false);
            result.put("message", response.getMsg());
        }

        return result;
    }

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public String handlePayNotify(String params) {
        try {
            logger.info("收到支付宝支付回调，参数：{}", params);
            Map<String, String> paramsMap = parseParams(params);
            logger.info("解析后的支付宝支付回调参数：{}", paramsMap);

            boolean signVerified = verifySign(paramsMap);
            if (!signVerified) {
                logger.error("支付宝支付回调签名验证失败，参数：{}", paramsMap);
                return "fail";
            }

            String tradeStatus = paramsMap.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                logger.error("支付宝支付回调交易状态不是成功，状态：{}", tradeStatus);
                return "fail";
            }

            String outTradeNo = paramsMap.get("out_trade_no");
            if (outTradeNo == null) {
                logger.error("支付宝支付回调中缺少 out_trade_no 参数，参数：{}", paramsMap);
                return "fail";
            }

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

    @Override
    public ResponseEntity<?> cancelOrder(Long orderId) {
        try {
            // 1. 获取订单信息（包含用户ID）
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("订单不存在");
            }
            Long userId = order.getUser_id();

            // 2. 取消订单
            int result = orderMapper.cancelOrder(orderId);
            if (result > 0) {
                // 3. 获取订单商品列表
                List<OrderItem> orderItems = orderItemMapper.getOrderItemsByOrderId(orderId);
                for (OrderItem orderItem : orderItems) {
                    // 4. 恢复商品库存
                    productMapper.updateProductStock(orderItem.getProduct_id(), orderItem.getQuantity());

                    // 5. 清除该商品的详情缓存
                    String productDetailsKey = PRODUCT_DETAILS_KEY + orderItem.getProduct_id();
                    stringRedisTemplate.delete(productDetailsKey);
                    log.info("清除商品详情缓存: {}", productDetailsKey);

                    // 6. 清除该用户的商品优惠缓存
                    if (userId != null) {
                        // 登录用户的优惠缓存
                        String userPromotionKey = PRODUCT_PROMOTIONS_KEY + orderItem.getProduct_id() + ":user:" + userId;
                        stringRedisTemplate.delete(userPromotionKey);
                        log.info("清除用户商品优惠缓存: {}", userPromotionKey);
                    }
                }

                return ResponseEntity.ok("订单取消成功");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("订单取消失败");
            }
        } catch (Exception e) {
            log.error("取消订单异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    public ResponseEntity<?> getOrdersByUserIdAndStatus(Integer userId, String status, int currentPage, int pageSize) {
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
                PageResult<OrderDisplay> PageResult = new PageResult<>(orderDisplayList, total, currentPage, pageSize, (int) Math.ceil((double) total / pageSize));
                return ResponseEntity.ok(PageResult);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未找到订单");
            }
        } catch (Exception e) {
            e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private boolean verifySign(Map<String, String> paramsMap) {
        try {
            return AlipaySignature.rsaCheckV1(
                    paramsMap,
                    alipayPublicKey,
                    "UTF-8",
                    "RSA2"
            );
        } catch (AlipayApiException e) {
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

    private String getPaymentMethodStr(Integer paymentType) {
        return switch (paymentType) {
            case 1 -> "WECHAT";
            case 2 -> "ALIPAY";
            default -> "OTHER";
        };
    }
}
