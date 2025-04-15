package com.example.backend.Impl.back;

import com.example.backend.Dao.back.ReviewBackMapper;
import com.example.backend.Entity.*;
import com.example.backend.Service.back.ReviewBackService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReviewBackServiceImpl implements ReviewBackService {

    @Autowired
    private ReviewBackMapper reviewBackMapper;

    public ResponseEntity<ProductReviewPage> SearchReviewList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("searchKeyword", searchKeyword);
            params.put("sortField", sortField);
            params.put("sortOrder", sortOrder);
            params.put("offset", (currentPage - 1) * pageSize);
            params.put("pageSize", pageSize);
            List<ProductReview> reviewList = reviewBackMapper.SearchReviewList(params);
            System.out.println("reviewList: " + reviewList);
            Map<String, Object> countParams = new HashMap<>();
            countParams.put("searchKeyword", searchKeyword);
            int total = reviewBackMapper.getSearchReviewTotal(countParams);
            System.out.println("total: " + total);
            if (total > 0) {
                return ResponseEntity.ok(new ProductReviewPage(reviewList, total));
            }else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    public ResponseEntity<ProductReview> getReviewDetailsBack(Long reviewId) {
        try {
            ProductReview productReview = reviewBackMapper.getReviewDetailsBackById(reviewId);
            if (productReview != null) {
                return ResponseEntity.ok().body(productReview);
            }else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public int deleteReview(Long reviewId) {
        return reviewBackMapper.deleteReview(reviewId);
    }

    @Override
    public int deleteReviewMore(List<Long> reviewIdList) {
        return reviewBackMapper.deleteReviewMore(reviewIdList);
    }

    @Override
    public boolean changeStatus(Integer reviewId, boolean status) {
        int res = reviewBackMapper.changeStatus(reviewId, status);
        return res > 0;
    }
}
