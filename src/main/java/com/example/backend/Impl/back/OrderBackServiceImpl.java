package com.example.backend.Impl.back;

import com.example.backend.Dao.*;
import com.example.backend.Dao.back.OrderBackMapper;
import com.example.backend.Dao.AddressMapper;
import com.example.backend.Entity.*;
import com.example.backend.Entity.back.OrderDetailsBack;
import com.example.backend.Entity.back.OrderResponsePageResultBack;
import com.example.backend.Service.back.OrderBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class OrderBackServiceImpl implements OrderBackService {
    @Autowired
    private OrderBackMapper orderBackMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductPromotionMapper productPromotionMapper;

    @Autowired
    private AddressMapper adderssMapper;

    public ResponseEntity<OrderResponsePageResultBack> SearchOrderList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("searchKeyword", searchKeyword);
            params.put("sortField", sortField);
            params.put("sortOrder", sortOrder);
            params.put("offset", (currentPage - 1) * pageSize);
            params.put("pageSize", pageSize);
            List<OrderDetailsBack> orderList = orderBackMapper.SearchOrderList(params);
            Map<String, Object> countParams = new HashMap<>();
            countParams.put("searchKeyword", searchKeyword);
            int total = orderBackMapper.getSearchOrderTotal(countParams);
            if (orderList != null) {
                List<OrderDetailsBack> responseList = orderList.stream().map(order -> {
                    Long orderId = order.getOrder_id();
                    if (order == null) {
                        throw new RuntimeException("Order not found with id: " + orderId);
                    }
                    Address address = adderssMapper.getAddressByOrderId(orderId);

                    List<OrderItem> orderItems = orderBackMapper.getOrderItemsByOrderId(orderId);
                    List<OrderItem> orderItemDetails = orderItems.stream().map(orderItem -> {
                        orderItem.setProduct(getProductPayInfo(orderItem.getProduct_id(), orderItem.getPromotion_id()));
                        return orderItem;
                    }).collect(Collectors.toList());

                    return new OrderDetailsBack(
                            order.getOrder_id(),
                            order.getOrder_no(),
                            order.getUser_id(),
                            order.getTotal_amount(),
                            order.getDiscount_amount(),
                            order.getPayment_amount(),
                            order.getOrder_status(),
                            address,
                            orderItemDetails,
                            order.getCreate_time(),
                            order.getUpdate_time(),
                            order.getPay_time(),
                            order.getExpress(),
                            order.getExpress_time()
                    );
                }).collect(Collectors.toList());
                OrderResponsePageResultBack orderResponsePageResultBack = new OrderResponsePageResultBack(responseList, total);
                return ResponseEntity.ok().body(orderResponsePageResultBack);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
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

    public ResponseEntity<OrderDetailsBack> getOrderDetailsBack(Long orderId) {
        try {
            OrderDetailsBack orderDetail = orderBackMapper.getOrderDetailsBackById(orderId);
            System.out.println(orderDetail);
            if (orderDetail != null) {
                List<OrderItem> orderItems = orderBackMapper.getOrderItemsByOrderId(orderId);
                List<OrderItem> orderItemDetails = orderItems.stream().map(orderItem -> {
                    orderItem.setProduct(getProductPayInfo(orderItem.getProduct_id(), orderItem.getPromotion_id()));
                    return orderItem;
                }).collect(Collectors.toList());
                orderDetail.setOrder_items(orderItemDetails);
                if (orderDetail.getAddress() == null) {
                    Address address = adderssMapper.getAddressByOrderId(orderId);
                    orderDetail.setAddress(address);
                }
                return ResponseEntity.ok().body(orderDetail);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    public ResponseEntity<?> ShipOrder(Long orderId, String express) {
        try {
            int res = orderBackMapper.ShipOrder(orderId, express);
            System.out.println(res);
            if (res == 1) {
                return ResponseEntity.ok().body("发货成功");
            } else {
                return ResponseEntity.status(404).body("发货失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e);
        }
    }
//    @Override
//    public int updateOrder(OrderDetailsBack order) {
//        try {
//            // 更新产品信息
//            int rowsAffected = orderBackMapper.updateOrder(order);
//            System.out.println("更新产品信息，受影响的行数: " + rowsAffected);
//
//            return 1;
//        } catch (Exception e) {
//            e.printStackTrace();
//            handleException(e);
//            return 0;
//        }
//    }

    private void handleException(Exception e) {
        if (e instanceof org.springframework.dao.DataAccessException) {
            System.out.println("数据库访问异常: " + e.getMessage());
        } else {
            System.out.println("其他异常: " + e.getMessage());
        }
    }
}
