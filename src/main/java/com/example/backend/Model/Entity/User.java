package com.example.backend.Model.Entity;

import com.example.backend.Model.Enum.LoginType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Integer id;
    private String username;
    private String email;
    private String phone;
    private String password;
    private String avatar;
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
    private String code;
    private String account;
    private boolean status;
    private Role role;
    private LoginType lastLoginType;

    public enum Role {
        Admin,
        Operator,
        User;
    }



}