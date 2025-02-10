package com.example.backend.Entity.ccc;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Categorys {
    private int category_id;
    private String category_name;
    private List<Brands> brands;
}
