package com.example.backend.Service;

import com.example.backend.Entity.ProductDetails;
import com.example.backend.Entity.ProductPromotion;
import com.example.backend.Entity.ProductResponse;
import com.example.backend.Entity.ProductResponsePageResult;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface ProductService {
    ResponseEntity<ProductDetails> getProductDetails(Long productId);
    ResponseEntity<List<Map<String, Object>>> selectNewList(int num);

    ResponseEntity<List<ProductPromotion>> selectFlashSalesList(int num);

    ResponseEntity<ProductResponsePageResult> selectApplePhoneProductList(int page, int size, String sortField, String sortOrder);
    ResponseEntity<ProductResponsePageResult> selectOrderPhoneProductList(int page, int size, String sortField, String sortOrder);

    ResponseEntity<ProductResponsePageResult> selectCategoryProductList(List<String> categoryName, int page, int size, String sortField, String sortOrder);

    ResponseEntity<ProductResponsePageResult> SearchProductList(String selectedCategory, String selectedBrand, String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);
}
