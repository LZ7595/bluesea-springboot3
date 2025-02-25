package com.example.backend.Service.back;

import com.example.backend.Entity.back.ProductDetailsBack;
import com.example.backend.Entity.back.ProductResponsePageResultBack;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProductBackService {

    ResponseEntity<ProductResponsePageResultBack> SearchProductList(String selectedCategory, String selectedBrand, String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    ResponseEntity<ProductDetailsBack> getProductDetailsBack(Long productId);

    int updateProduct(ProductDetailsBack product);
    String uploadFile(org.springframework.web.multipart.MultipartFile file);

    int addProduct(ProductDetailsBack product);
    int deleteProduct(Long product_id);
    int deleteProductMore(List<Long> productIdList);
}
