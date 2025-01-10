package com.example.backend.Entity;

import com.example.backend.Entity.Enum.LoginType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private int id;
    private String username;
    private String email;
    private String password;
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
    private String code;
    private LocalDateTime codeExpiration;
    private Role role;
    private LoginType lastLoginType;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public LocalDateTime getCodeExpiration() {
        return codeExpiration;
    }

    public void setCodeExpiration(LocalDateTime codeExpiration) {
        this.codeExpiration = codeExpiration;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LoginType getLastLoginType() {
        return lastLoginType;
    }

    public void setLastLoginType(LoginType lastLoginType) {
        this.lastLoginType = lastLoginType;
    }


    public enum Role {
        Director,
        Admin,
        User;
    }



}