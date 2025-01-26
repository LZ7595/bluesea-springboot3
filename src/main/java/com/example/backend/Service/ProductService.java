package com.example.backend.Service;

import com.example.backend.Entity.Product;
import com.example.backend.Entity.ProductResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface ProductService {
    ResponseEntity<ProductResponse> selectProductDetails(Long id);

    ResponseEntity<List<Map<String, Object>>> selectNewList(int num);

    ResponseEntity<List<Map<String, Object>>> selectFlashSalesList(int num);

    ResponseEntity<List<ProductResponse>> selectApplePhoneProductList(int num);

    ResponseEntity<List<ProductResponse>> selectOrderPhoneProductList(int num);
}
