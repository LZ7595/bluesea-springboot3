package com.example.backend.Controller;

import com.example.backend.Entity.Enum.ErrorType;
import com.example.backend.Entity.Enum.LoginType;
import com.example.backend.Entity.User;
import com.example.backend.Service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/sendCode")
    public ResponseEntity<?> sendVerificationCode(@RequestParam String email, @RequestParam int type) {
        System.out.println("接收到的email值为：" + email);
        System.out.println("接收到的type值为：" + type);
        return authService.sendVerificationCode(email, type);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        System.out.println("接收到的user值为：" + user);

        return authService.registerUser(user);
    }
    @PostMapping("/login")
    public ResponseEntity loginUser(@RequestBody User user ,@RequestParam LoginType type) {
        System.out.println("接收到的type值为：" + type);
        System.out.println("接收到的user值为：" + user);
        return authService.loginUser(user,type);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return authService.logoutUser();
    }
}