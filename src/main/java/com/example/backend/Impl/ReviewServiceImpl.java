package com.example.backend.Impl;

import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Entity.ProductReview;
import com.example.backend.Service.ReviewService;
import com.example.backend.Dao.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Override
    public int addReview(ProductReview review) {
        try {
            int result = reviewMapper.addReview(review);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int updateReview(ProductReview review) {
        try {
            int result = reviewMapper.updateReview(review);
            return result;
        }catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public List<ProductReview> getReviewsByOrderId(Integer orderId) {
        return reviewMapper.getReviewsByOrderId(orderId);
    }

    @Override
    public ResponseEntity<?> getReviewsByProductId(Long productId, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<ProductReview> reviews = reviewMapper.getReviewsByProductId(productId, offset, pageSize);
        int total = reviewMapper.getReviewCountByProductId(productId);
        PageResult<ProductReview> PageResult = new PageResult<>(reviews, total,currentPage, pageSize, (int) Math.ceil((double) total / pageSize));
        return ResponseEntity.ok(PageResult);
    }
}
