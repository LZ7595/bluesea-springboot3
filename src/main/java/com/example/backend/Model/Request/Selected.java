package com.example.backend.Model.Request;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Selected {
    private Integer cartId;
    private Integer isSelected;
}
