package com.example.backend.Service.back;

import com.example.backend.Entity.ProductPromotion;
import com.example.backend.Entity.back.PromotionBack;
import com.example.backend.Entity.back.PromotionResponsePageResultBack;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PromotionBackService {

    ResponseEntity<PromotionResponsePageResultBack> SearchPromotionList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    ResponseEntity<PromotionBack> getPromotionDetailsBack(Long promotionId);

    ResponseEntity<?> getProductDetailBack(Long productId);
    int updatePromotion(PromotionBack promotion);

    int addPromotion(PromotionBack promotion);
    int deletePromotion(Long promotion_id);
    int deletePromotionMore(List<Long> promotionIdList);
}
