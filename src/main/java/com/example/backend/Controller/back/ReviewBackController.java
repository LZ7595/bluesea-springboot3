package com.example.backend.Controller.back;

import com.example.backend.Entity.ProductReview;
import com.example.backend.Entity.ProductReviewPage;
import com.example.backend.Service.back.ReviewBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/back/comment")
public class ReviewBackController {

    @Autowired
    private ReviewBackService reviewBackService;

    @GetMapping("/search")
    public ResponseEntity<ProductReviewPage> SearchReviewList(
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "review_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return reviewBackService.SearchReviewList(searchKeyword, sortField, sortOrder, currentPage, pageSize);
    }

    @GetMapping("/details")
    public ResponseEntity<?> DetailBack(@RequestParam Long review_id) {
        return reviewBackService.getReviewDetailsBack(review_id);
    }

    @DeleteMapping("/deleteone")
    public String deleteReview(@RequestParam Long review_id) {
        int result = reviewBackService.deleteReview(review_id);
        if (result > 0) {
            return "评论删除成功";
        } else {
            return "评论删除失败";
        }
    }

    @DeleteMapping("/deletemore")
    public String deleteReviewMore(@RequestBody List<Long> reviewIdList) {
        try {
            // 调用服务层方法处理删除逻辑
            reviewBackService.deleteReviewMore(reviewIdList);
            return "评论删除成功";
        } catch (Exception e) {
            return "评论删除失败: " + e.getMessage();
        }
    }
    @PutMapping("/changestatus")
    public String changeStatus(@RequestBody ProductReview productReview) {
        try {
        // 调用服务层方法处理删除逻辑
            boolean res = reviewBackService.changeStatus(productReview.getReview_id(), productReview.isStatus());
            if (res) {
                return "评论状态修改成功";
            }else {
                return "评论状态修改失败";
            }
        }catch (Exception e){
            return "评论状态修改失败: " + e.getMessage();
        }
    }
}