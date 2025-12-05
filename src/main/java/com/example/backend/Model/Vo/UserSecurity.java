package com.example.backend.Model.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSecurity {
    private Integer id;
    private String username;
    private String phone;
    private String email;
}
