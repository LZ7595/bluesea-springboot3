package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetails {
    private Integer id;
    private Integer userId;
    private java.sql.Date birthday;
    private String gender;
    private String avatar;
}
