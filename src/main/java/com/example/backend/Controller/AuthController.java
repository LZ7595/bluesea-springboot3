package com.example.backend.Controller;

import com.example.backend.Entity.Enum.ErrorType;
import com.example.backend.Entity.Enum.LoginType;
import com.example.backend.Entity.User;
import com.example.backend.Service.AuthService;
import com.example.backend.Utils.Jwt;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> loginUser(
            @RequestBody User user,
            @RequestParam LoginType type,
            HttpServletRequest request // 用于获取客户端类型
    ) {
        System.out.println("接收到的type值为：" + type);
        System.out.println("接收到的user值为：" + user);

        // 调用服务层，传入客户端类型
        return authService.loginUser(user, type, request);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return authService.logoutUser(request);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,  // 用于从Cookie提取refreshToken（App/H5）
            @RequestBody(required = false) Map<String, String> requestBody  // 兼容可能的Body参数
    ) {
        String refreshToken = null;
        String clientType = request.getHeader("Client-Type");  // 获取客户端类型

        // 1. 根据客户端类型提取refreshToken
        if ("miniprogram".equals(clientType)) {
            // 小程序：从Authorization头提取（格式：Bearer refreshToken=xxx）
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                refreshToken = authHeader.substring(7).trim();
            }
        } else {
            // App/H5：优先从Cookie提取（前端通过Cookie传递）
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
            // 兼容可能的Body参数传递（防止极端情况）
            if (refreshToken == null && requestBody != null) {
                refreshToken = requestBody.get("refreshToken");
            }
        }

        // 2. 校验refreshToken是否存在
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ErrorType.REFRESH_TOKEN_MISSING.toErrorResponse());
        }

        try {
            // 3. 调用服务层刷新令牌
            String newAccessToken = authService.refreshToken(refreshToken);

            // 4. 生成新的refreshToken（可选，增强安全性）
            // 注意：若启用此逻辑，需同步修改Redis中refreshToken的映射关系
            String newRefreshToken = null;
            // if (需要生成新的refreshToken) {
            //     newRefreshToken = jwt.generateRefreshToken(userId);
            //     // 更新Redis：删除旧refreshToken映射，添加新映射
            // }

            // 5. 构建响应（适配多端）
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("expiresIn", accessTokenExpirationTime / 1000);
            // 小程序需要显式返回refreshToken（App/H5通过Cookie自动更新）
            if ("miniprogram".equals(clientType) && newRefreshToken != null) {
                response.put("refreshToken", newRefreshToken);
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ErrorType.REFRESH_TOKEN_INVALID.toErrorResponse());
        }
    }
}