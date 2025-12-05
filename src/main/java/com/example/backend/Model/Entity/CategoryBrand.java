package com.example.backend.Model.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBrand {
    private Long id;
    private Long brand_id;
    private Long category_id;
    private String category_name;
    private String brand_name;
    private String logo;
    private boolean status;
    private Date create_time;
    private Date update_time;
}
