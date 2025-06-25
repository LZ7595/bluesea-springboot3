package com.example.backend.Utils;

import com.example.backend.Service.AuthService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private long accessTokenExpirationTime;
    private Jwt jwt;
    private StringRedisTemplate stringRedisTemplate;
    private AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);
    private static final double RENEW_THRESHOLD = 0.3;

    // 添加无参构造函数
    public JwtInterceptor() {
    }

    // 带参构造函数
    public JwtInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 使用 setter 方法注入其他依赖
    @Autowired
    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    @Autowired
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @Value("${jwt.access.expiration}")
    public void setAccessTokenExpirationTime(long accessTokenExpirationTime) {
        this.accessTokenExpirationTime = accessTokenExpirationTime;
    }
    public String getCookieValue(HttpServletRequest request, String cookieName) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String accessToken = getCookieValue(request, "accessToken");
        String refreshToken = getCookieValue(request, "refreshToken");
        // 检查访问令牌
        if (accessToken != null && jwt.validateAccessToken(accessToken)) {
            // 将用户信息存入请求属性
            setUserAttributes(request, accessToken);

            // 检查是否需要续约
            if (jwt.isTokenAboutToExpire(accessToken, RENEW_THRESHOLD)) {
                try {
                    // 访问令牌即将过期，尝试续约
                    if (refreshToken != null && jwt.validateRefreshToken(refreshToken)) {
                        String newAccessToken = authService.refreshToken(refreshToken);
                        if (newAccessToken != null) {
                            // 更新访问令牌Cookie
                            updateAccessTokenCookie(response, newAccessToken);
                            logger.info("访问令牌已自动续约");
                        }
                    }
                } catch (Exception e) {
                    logger.error("续约访问令牌失败", e);
                    // 续约失败不影响本次请求，继续处理
                }
            }

            return true;
        }

        // 访问令牌无效，尝试刷新
        if (refreshToken != null && jwt.validateRefreshToken(refreshToken)) {
            try {
                String newAccessToken = authService.refreshToken(refreshToken);
                if (newAccessToken != null) {
                    updateAccessTokenCookie(response, newAccessToken);
                    setUserAttributes(request, newAccessToken);
                    return true;
                }
            } catch (Exception e) {
                logger.error("刷新令牌失败", e);
            }
        }

        // 未授权
        handleUnauthorized(response);
        return false;
    }
    // 从令牌中提取用户信息并设置到请求属性
    private void setUserAttributes(HttpServletRequest request, String token) {
        request.setAttribute("userId", jwt.getIdFromToken(token));
        request.setAttribute("username", jwt.getUsernameFromToken(token));
        request.setAttribute("role", jwt.getRoleFromToken(token));
    }


    // 更新访问令牌Cookie
    private void updateAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .maxAge(accessTokenExpirationTime / 1000)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 处理未授权请求
    private void handleUnauthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            Map<String, String> result = new HashMap<>();
            result.put("code", "401");
            result.put("message", "未授权，请登录");
            PrintWriter writer = response.getWriter();
            writer.write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
            writer.flush();
        } catch (IOException e) {
            logger.error("处理未授权请求时发生错误", e);
        }
    }
}