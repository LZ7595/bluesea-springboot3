package com.example.backend.Entity;

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
