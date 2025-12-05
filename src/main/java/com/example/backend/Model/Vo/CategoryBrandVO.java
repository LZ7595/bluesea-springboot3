package com.example.backend.Model.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBrandVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer category_id;
    private String category_name;
    private List<BrandVO> brands;
}