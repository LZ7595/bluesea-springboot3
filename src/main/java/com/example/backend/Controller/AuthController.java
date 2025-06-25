package com.example.backend.Controller;

import com.example.backend.Entity.Enum.ErrorType;
import com.example.backend.Entity.Enum.LoginType;
import com.example.backend.Entity.User;
import com.example.backend.Service.AuthService;
import com.example.backend.Utils.Jwt;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${jwt.access.expiration}")
    private long accessTokenExpirationTime;

    @Resource
    private Jwt jwt;
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
    public ResponseEntity<?> loginUser(@RequestBody User user, @RequestParam LoginType type) {
        System.out.println("接收到的type值为：" + type);
        System.out.println("接收到的user值为：" + user);
        return authService.loginUser(user, type);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return authService.logoutUser();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(ErrorType.REFRESH_TOKEN_MISSING.toErrorResponse());
        }

        try {
            // 调用服务层刷新令牌
            String newAccessToken = authService.refreshToken(refreshToken);

            // 返回新的访问令牌
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("expiresIn", accessTokenExpirationTime  / 1000);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ErrorType.REFRESH_TOKEN_INVALID.toErrorResponse());
        }
    }
}