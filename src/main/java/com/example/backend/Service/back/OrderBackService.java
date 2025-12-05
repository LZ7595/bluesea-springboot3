package com.example.backend.Service.back;

import com.example.backend.Model.Entity.back.OrderDetailsBack;
import org.springframework.http.ResponseEntity;


public interface OrderBackService {

    ResponseEntity<?> SearchOrderList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    ResponseEntity<OrderDetailsBack> getOrderDetailsBack(Long orderId);

    ResponseEntity<?> ShipOrder(Long orderId, int express_id ,String express_num);

//    int updateOrder(OrderDetailsBack product);
}
