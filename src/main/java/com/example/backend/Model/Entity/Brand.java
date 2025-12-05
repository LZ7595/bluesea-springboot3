package com.example.backend.Model.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Brand {
    private Long brand_id;
    private String brand_name;
    private String brand_description;
    private String logo;
    private boolean status;
    private Date create_time;
    private Date update_time;
    private int order_num;
}

