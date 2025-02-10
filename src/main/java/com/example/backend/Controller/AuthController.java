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
    public ResponseEntity registerUser(@RequestBody User user) {
        System.out.println("接收到的user值为：" + user);
        // 检查用户名和邮箱是否已被注册
        if (authService.isUsernameUsed(user.getUsername())) {
            return ResponseEntity.status(409).body(ErrorType.USERNAME_REGISTERED.toErrorResponse()); // 用户名已注册过
        }

        if (authService.isEmailUsed(user.getEmail())) {
            return ResponseEntity.status(409).body(ErrorType.EMAIL_REGISTERED.toErrorResponse()); // 邮箱已注册过
        }
        // 验证验证码
        if (!authService.validateCode(user.getEmail(), user.getCode())) {
            return ResponseEntity.status(401).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse()); // 验证码错误
        }
        ResponseEntity registeredUser = authService.registerUser(user);
        if (registeredUser == null) {
            return ResponseEntity.status(400).body(ErrorType.REGISTER_FAILED.toErrorResponse()); // 注册失败
        }
        return ResponseEntity.ok(authService.registerUser(user));
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