package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Banner {
    private Long id;
    private String image;
    private Long product_id;
    private String product_name;
    private boolean status;
    private Date create_time;
    private Date update_time;
}
