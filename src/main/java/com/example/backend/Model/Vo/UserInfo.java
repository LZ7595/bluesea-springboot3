package com.example.backend.Model.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private Integer id;
    private String username;
    private java.sql.Date birthday;
    private String email;
    private String phone;
    private java.sql.Date register_time;
    private String gender;
    private String avatar;
    private String role;
    private String status;
}
