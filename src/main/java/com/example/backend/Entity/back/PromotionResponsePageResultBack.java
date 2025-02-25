package com.example.backend.Entity.back;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromotionResponsePageResultBack {
    private List<PromotionBack> promotionResponseList;
    private int total;
}