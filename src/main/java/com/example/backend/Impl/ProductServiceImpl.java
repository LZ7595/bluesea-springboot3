package com.example.backend.Impl;

import com.example.backend.Dao.ProductMapper;
import com.example.backend.Entity.FlashSale;
import com.example.backend.Entity.Product;
import com.example.backend.Entity.ProductResponse;
import com.example.backend.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    public ResponseEntity<ProductResponse> selectProductDetails(Long id) {
        try {
            Product productDetails = productMapper.getProductById(id);
            if (productDetails != null) {
                FlashSale flashSale = productMapper.getFlashSaleByProductId(id);
                ProductResponse response = new ProductResponse(productDetails, flashSale);
                return ResponseEntity.ok().body(response);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @Override
    public ResponseEntity<List<Map<String, Object>>> selectNewList(int num) {
        try {
            List<Product> newList = productMapper.getNewList(num);
            System.out.println(newList);

            if (newList != null) {
                List<Map<String, Object>> infoList = new ArrayList<>();
                for (Product product : newList) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("product_id", product.getProduct_id());
                    info.put("product_name", product.getProduct_name());
                    info.put("price", product.getPrice());
                    info.put("quality", product.getQuality());
                    info.put("image_url", product.getImage_url());
                    infoList.add(info);
                }
                return ResponseEntity.ok().body(infoList);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public ResponseEntity<List<Map<String, Object>>> selectFlashSalesList(int num) {
        try {
            List<FlashSale> flashSales = productMapper.getFlashSalesList(num);
            System.out.println(flashSales);

            if (flashSales != null) {
                List<Map<String, Object>> infoList = new ArrayList<>();
                for (FlashSale flashSale : flashSales) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("product_id", flashSale.getProduct_id());
                    info.put("flash_sale_id", flashSale.getFlash_sale_id());
                    info.put("product_name", flashSale.getProduct_name());
                    info.put("price", flashSale.getPrice());
                    info.put("discount_price", flashSale.getDiscount_price());
                    info.put("max_quantity", flashSale.getMax_quantity());
                    info.put("sold_quantity", flashSale.getSold_quantity());
                    info.put("image_url", flashSale.getImage_url());
                    info.put("quality", flashSale.getQuality());
                    infoList.add(info);
                }
                return ResponseEntity.ok().body(infoList);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public ResponseEntity<List<ProductResponse>> selectApplePhoneProductList(int num){
        try {
            List<Product> applePhoneProductList = productMapper.getApplePhoneProductList(num);
            if (applePhoneProductList != null){
                List<ProductResponse> responseList = applePhoneProductList.stream().map(product -> {
                    FlashSale flashSale = productMapper.getFlashSaleByProductId(product.getProduct_id());
                    return new ProductResponse(product, flashSale);
                }).collect(Collectors.toList());
            return ResponseEntity.ok().body(responseList);
        }
    }catch (Exception e){
        return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }
    @Override
    public ResponseEntity<List<ProductResponse>> selectOrderPhoneProductList(int num){
        try {
            List<Product> OtherPhoneProductList = productMapper.getOtherPhoneProductList(num);
            if (OtherPhoneProductList != null){
                List<ProductResponse> responseList = OtherPhoneProductList.stream().map(product -> {
                    FlashSale flashSale = productMapper.getFlashSaleByProductId(product.getProduct_id());
                    return new ProductResponse(product, flashSale);
                }).collect(Collectors.toList());
            return ResponseEntity.ok().body(responseList);
        }
    }catch (Exception e){
        return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }
}
