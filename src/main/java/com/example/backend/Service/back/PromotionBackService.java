package com.example.backend.Service.back;

import com.example.backend.Model.Entity.back.PromotionBack;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PromotionBackService {

    ResponseEntity<?> SearchPromotionList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    ResponseEntity<PromotionBack> getPromotionDetailsBack(Long promotionId);

    ResponseEntity<?> getProductDetailBack(Long productId);
    int updatePromotion(PromotionBack promotion);

    int addPromotion(PromotionBack promotion);
    int deletePromotion(Long promotion_id);
    int deletePromotionMore(List<Long> promotionIdList);
}
