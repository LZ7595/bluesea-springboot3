package com.example.backend.Entity.back;

import com.example.backend.Entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsBack {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private java.sql.Date birthday;
    private User.Role role;
    private boolean status;
    private String gender;
    private String avatar;
    private String last_login_type;
    private LocalDateTime register_time;
    private LocalDateTime last_login_time;
}
