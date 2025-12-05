package com.example.backend.Service.back;

import com.example.backend.Model.Entity.ProductReview;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ReviewBackService {

    ResponseEntity<?> SearchReviewList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    ResponseEntity<ProductReview> getReviewDetailsBack(Long reviewId);

    int deleteReview(Long reviewId);
    int deleteReviewMore(List<Long> reviewIdList);

    boolean changeStatus(Integer reviewId, boolean status);
}
