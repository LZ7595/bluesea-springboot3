package com.example.backend.Service;

import com.example.backend.Entity.ProductReview;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ReviewService {
    int addReview(ProductReview review);

    int updateReview(ProductReview review);
    List<ProductReview> getReviewsByOrderId(Integer orderId);

    ResponseEntity<?> getReviewsByProductId(Integer productId, int currentPage, int pageSize);
}
