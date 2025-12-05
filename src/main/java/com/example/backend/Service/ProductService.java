package com.example.backend.Service;

import com.example.backend.Model.Vo.ProductDetails;
import com.example.backend.Model.Vo.ProductPayInfo;
import com.example.backend.Model.Entity.ProductPromotion;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface ProductService {
    ResponseEntity<ProductDetails> getProductDetails(Long productId, Integer userId);
    ResponseEntity<List<Map<String, Object>>> selectNewList(int num);

    ResponseEntity<List<ProductPromotion>> selectFlashSalesList(int num);

    ResponseEntity<?> selectApplePhoneProductList(int page, int size, String sortField, String sortOrder);
    ResponseEntity<?> selectOrderPhoneProductList(int page, int size, String sortField, String sortOrder);

    ResponseEntity<?> selectCategoryProductList(List<String> categoryName, int page, int size, String sortField, String sortOrder);

    ResponseEntity<?> SearchProductList(Integer selectedCategory, Integer selectedBrand, String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    List<ProductPayInfo> batchGetProductDetails(List<Long> productIds, Integer userId);

}
