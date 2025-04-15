package com.example.backend.Service;

import com.example.backend.Entity.ProductReview;
import com.example.backend.Entity.ProductReviewPage;

import java.util.List;

public interface ReviewService {
    int addReview(ProductReview review);

    int updateReview(ProductReview review);
    List<ProductReview> getReviewsByOrderId(Integer orderId);

    ProductReviewPage getReviewsByProductId(Integer productId, int currentPage, int pageSize);
}
