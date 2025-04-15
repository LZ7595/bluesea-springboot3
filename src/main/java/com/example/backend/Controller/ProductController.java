package com.example.backend.Controller;

import com.example.backend.Entity.*;
import com.example.backend.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/details")
    public ResponseEntity<ProductDetails> getProductDetails(@RequestParam Long product_id) {
        return productService.getProductDetails(product_id);
    }
    @GetMapping("/newList")
    public ResponseEntity<List<Map<String, Object>>> getNewList(@RequestParam int num) {
        return productService.selectNewList(num);
    }
@GetMapping("/flashSalesList")
public ResponseEntity<List<ProductPromotion>> getFlashSalesList(@RequestParam int num) {
    return productService.selectFlashSalesList(num);
}
    @GetMapping("/applePhoneList")
    public ResponseEntity<ProductResponsePageResult> getAppleProductList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        return productService.selectApplePhoneProductList(page, pageSize, sortField, sortOrder);
    }

    @GetMapping("/orderPhoneList")
    public ResponseEntity<ProductResponsePageResult> getOrderPhoneList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        return productService.selectOrderPhoneProductList(page, pageSize, sortField, sortOrder);
    }
    @GetMapping("/padList")
    public ResponseEntity<ProductResponsePageResult> getPadList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        List<String> categoryNames = Arrays.asList("平板");
        return productService.selectCategoryProductList(categoryNames,page, pageSize, sortField, sortOrder);
    }
    @GetMapping("/computerList")
    public ResponseEntity<ProductResponsePageResult> getComputerList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        List<String> categoryNames = Arrays.asList("电脑");
        return productService.selectCategoryProductList(categoryNames,page, pageSize, sortField, sortOrder);
    }
    @GetMapping("/lifeList")
    public ResponseEntity<ProductResponsePageResult> getLifeListList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        List<String> categoryNames = Arrays.asList("运动户外", "穿戴", "摄影摄像");
        return productService.selectCategoryProductList(categoryNames,page, pageSize, sortField, sortOrder);
    }
    @GetMapping("/search")
    public ResponseEntity<ProductResponsePageResult> SearchProductList(
            @RequestParam(required = false) String selectedCategory,
            @RequestParam(required = false) String selectedBrand,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return productService.SearchProductList(selectedCategory, selectedBrand, searchKeyword, sortField, sortOrder, currentPage, pageSize);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProductPayInfo>> batchGetProductDetails(@RequestBody List<Long> productIds,@RequestParam Integer userId) {
        List<ProductPayInfo> productPayInfos = productService.batchGetProductDetails(productIds,userId);
        return ResponseEntity.ok(productPayInfos);
    }
}
