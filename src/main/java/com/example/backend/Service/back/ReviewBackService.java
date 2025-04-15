package com.example.backend.Service.back;

import com.example.backend.Entity.ProductReview;
import com.example.backend.Entity.ProductReviewPage;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ReviewBackService {

    ResponseEntity<ProductReviewPage> SearchReviewList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    ResponseEntity<ProductReview> getReviewDetailsBack(Long reviewId);

    int deleteReview(Long reviewId);
    int deleteReviewMore(List<Long> reviewIdList);

    boolean changeStatus(Integer reviewId, boolean status);
}
