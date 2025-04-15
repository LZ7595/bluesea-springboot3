package com.example.backend.Controller.back;

import com.example.backend.Entity.back.OrderDetailsBack;
import com.example.backend.Entity.back.OrderResponsePageResultBack;
import com.example.backend.Entity.back.ProductDetailsBack;
import com.example.backend.Entity.back.ProductResponsePageResultBack;
import com.example.backend.Service.back.OrderBackService;
import com.example.backend.Service.back.ProductBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/back/order")
public class OrderBackController {

    @Autowired
    private OrderBackService orderBackService;
    @GetMapping("/search")
    public ResponseEntity<OrderResponsePageResultBack> SearchOrderList(
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return orderBackService.SearchOrderList(searchKeyword, sortField, sortOrder, currentPage, pageSize);
    }

    @GetMapping("/details")
    public ResponseEntity<?> DetailBack(@RequestParam Long order_id) {
        return orderBackService.getOrderDetailsBack(order_id);
    }

    @PostMapping("/ship")
    public ResponseEntity<?> ShipOrder(@RequestParam Long order_id,@RequestParam String express) {
        return orderBackService.ShipOrder(order_id,express);
    }
//    @PutMapping("/update")
//    public String updateProduct(@RequestBody OrderDetailsBack order) {
//        order.setUpdate_time(new Date());
//        int result = orderBackService.updateOrder(order);
//        if (result > 0) {
//            return "商品信息修改成功";
//        } else {
//            return "商品信息修改失败";
//        }
//    }
}
