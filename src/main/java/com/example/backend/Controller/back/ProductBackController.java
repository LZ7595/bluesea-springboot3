package com.example.backend.Controller.back;

import com.example.backend.Entity.back.ProductDetailsBack;
import com.example.backend.Entity.back.ProductResponsePageResultBack;
import com.example.backend.Service.back.ProductBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/back/product")
public class ProductBackController {

    @Autowired
    private ProductBackService productBackService;
    @GetMapping("/search")
    public ResponseEntity<ProductResponsePageResultBack> SearchProductList(
            @RequestParam(required = false) String selectedCategory,
            @RequestParam(required = false) String selectedBrand,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return productBackService.SearchProductList(selectedCategory, selectedBrand, searchKeyword, sortField, sortOrder, currentPage, pageSize);
    }

    @GetMapping("/details")
    public ResponseEntity<?> DetailBack(@RequestParam Long product_id) {
        return productBackService.getProductDetailsBack(product_id);
    }

    @GetMapping("/selectList")
    public ResponseEntity<?> getSelectList() {
        return productBackService.getSelectList();
    }

    @PutMapping("/update")
    public String updateProduct(@RequestBody ProductDetailsBack product) {
        System.out.println(product);
        product.setUpdate_time(new Date());
        int result = productBackService.updateProduct(product);
        if (result > 0) {
            return "商品信息修改成功";
        } else {
            return "商品信息修改失败";
        }
    }

    @PostMapping("/add")
    public String addProduct(@RequestBody ProductDetailsBack product) {
        System.out.println(product);
        int result = productBackService.addProduct(product);
        if (result > 0) {
            return "商品信息添加成功";
        } else {
            return "商品信息添加失败";
        }
    }

    @DeleteMapping("/deleteone")
    public String deleteProduct(@RequestParam Long product_id) {
        int result = productBackService.deleteProduct(product_id);
        if (result > 0) {
            return "商品信息删除成功";
        } else {
            return "商品信息删除失败";
        }
    }
    @DeleteMapping("/deletemore")
    public String deleteProductMore(@RequestBody List<Long> productIdList) {
        try {
            // 调用服务层方法处理删除逻辑
            productBackService.deleteProductMore(productIdList);
            return "商品删除成功";
        } catch (Exception e) {
            return "商品删除失败: " + e.getMessage();
        }
    }
}
