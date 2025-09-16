package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrandVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer brand_id;
    private String brand_name;
    private String logo;
    private Integer category_id; // 通过关联表查询时添加
}