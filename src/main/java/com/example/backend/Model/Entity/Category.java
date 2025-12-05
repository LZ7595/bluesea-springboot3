package com.example.backend.Model.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Category {
    private Long category_id;
    private String category_name;
    private int parent_category_id;
    private String image_url;
    private Integer status;
    private Integer order_num;
    private Boolean hasChildren;
    private List<Category> children;
    private Date create_time;
    private Date update_time;
}
