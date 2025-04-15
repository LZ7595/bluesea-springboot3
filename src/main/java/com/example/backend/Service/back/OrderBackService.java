package com.example.backend.Service.back;

import com.example.backend.Entity.back.OrderDetailsBack;
import com.example.backend.Entity.back.OrderResponsePageResultBack;
import org.springframework.http.ResponseEntity;


public interface OrderBackService {

    ResponseEntity<OrderResponsePageResultBack> SearchOrderList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    ResponseEntity<OrderDetailsBack> getOrderDetailsBack(Long orderId);

    ResponseEntity<?> ShipOrder(Long orderId,String express);

//    int updateOrder(OrderDetailsBack product);
}
