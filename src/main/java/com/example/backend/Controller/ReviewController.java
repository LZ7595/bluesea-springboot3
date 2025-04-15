package com.example.backend.Controller;

import com.example.backend.Entity.ProductReview;
import com.example.backend.Entity.ProductReviewPage;
import com.example.backend.Service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/review")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;

    @PostMapping("/add")
    public String addReview(@RequestBody ProductReview review) {
        int result = reviewService.addReview(review);
        if (result > 0) {
            return "评论添加成功";
        } else {
            return "评论添加失败";
        }
    }

    @PutMapping("/update")
    public String updateReview(@RequestBody ProductReview review) {
        System.out.println("Received review: " + review);
        int result = reviewService.updateReview(review);
        if (result > 0) {
            return "评论更新成功";
        } else {
            return "评论更新失败";
        }
    }

    @GetMapping("/order/{orderId}")
    public List<ProductReview> getReviewsByOrderId(@PathVariable("orderId") Integer orderId) {
        List<ProductReview> reviews = reviewService.getReviewsByOrderId(orderId);
        return reviews;
    }

    @GetMapping("/product/{productId}")
    public ProductReviewPage getReviewsByProductId(@PathVariable("productId") Integer productId,
                                                   @RequestParam(defaultValue = "1") int currentPage,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        ProductReviewPage ProductReviewPageRes = reviewService.getReviewsByProductId(productId,currentPage, pageSize);
        return ProductReviewPageRes;
    }
}
