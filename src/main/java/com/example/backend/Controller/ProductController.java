package com.example.backend.Controller;

import com.example.backend.Entity.Product;
import com.example.backend.Entity.ProductResponse;
import com.example.backend.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/details")
    public ResponseEntity<ProductResponse> getProductDetails(@RequestParam Long product_id) {
        return productService.selectProductDetails(product_id);
    }
    @GetMapping("/newList")
    public ResponseEntity<List<Map<String, Object>>> getNewList(@RequestParam int num) {
        return productService.selectNewList(num);
    }
    @GetMapping("/flashSalesList")
    public ResponseEntity<List<Map<String, Object>>> getFlashSalesList(@RequestParam int num) {
        return productService.selectFlashSalesList(num);
    }
    @GetMapping("/applePhoneList")
    public ResponseEntity<List<ProductResponse>> getAppleProductList(@RequestParam int num) {
        return productService.selectApplePhoneProductList(num);
    }

    @GetMapping("/orderPhoneList")
    public ResponseEntity<List<ProductResponse>> getOrderPhoneList(@RequestParam int num) {
        return productService.selectOrderPhoneProductList(num);
    }
}
