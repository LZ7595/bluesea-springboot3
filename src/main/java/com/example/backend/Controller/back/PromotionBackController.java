package com.example.backend.Controller.back;

import com.example.backend.Entity.back.PromotionBack;
import com.example.backend.Entity.back.PromotionResponsePageResultBack;
import com.example.backend.Service.back.PromotionBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/back/promotion")
public class PromotionBackController {

    @Autowired
    private PromotionBackService promotionBackService;
    @GetMapping("/search")
    public ResponseEntity<PromotionResponsePageResultBack> SearchPromotionList(
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return promotionBackService.SearchPromotionList(searchKeyword, sortField, sortOrder, currentPage, pageSize);
    }

    @GetMapping("/details")
    public ResponseEntity<?> DetailBack(@RequestParam Long promotion_id) {
        return promotionBackService.getPromotionDetailsBack(promotion_id);
    }
    @GetMapping("/productdetail")
    public ResponseEntity<?> SearchDetailProductBack(@RequestParam Long product_id) {
        return promotionBackService.getProductDetailBack(product_id);
    }
    @PutMapping("/update")
    public String updatePromotion(@RequestBody PromotionBack promotion) {
        System.out.println(promotion);
        promotion.setUpdate_time(new Date());
        int result = promotionBackService.updatePromotion(promotion);
        if (result > 0) {
            return "商品信息修改成功";
        } else {
            return "商品信息修改失败";
        }
    }

    @PostMapping("/add")
    public String addPromotion(@RequestBody PromotionBack promotion) {
        System.out.println(promotion);
        int result = promotionBackService.addPromotion(promotion);
        if (result > 0) {
            return "商品信息添加成功";
        } else {
            return "商品信息添加失败";
        }
    }

    @DeleteMapping("/deleteone")
    public String deletePromotion(@RequestParam Long promotion_id) {
        int result = promotionBackService.deletePromotion(promotion_id);
        if (result > 0) {
            return "商品信息删除成功";
        } else {
            return "商品信息删除失败";
        }
    }
    @DeleteMapping("/deletemore")
    public String deletePromotionMore(@RequestBody List<Long> promotionIdList) {
        try {
            // 调用服务层方法处理删除逻辑
            promotionBackService.deletePromotionMore(promotionIdList);
            return "商品删除成功";
        } catch (Exception e) {
            return "商品删除失败: " + e.getMessage();
        }
    }
}
