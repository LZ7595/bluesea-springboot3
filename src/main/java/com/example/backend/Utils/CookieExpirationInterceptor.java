package com.example.backend.Utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class CookieExpirationInterceptor implements HandlerInterceptor {


    @Autowired
    private Jwt jwt;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求中的 cookies
        Cookie[] cookies = request.getCookies();
        boolean validTokenFound = false;


        if (cookies!= null) {
            for (Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    // 验证 JWT 令牌是否过期
                    if (jwt.validateAccessToken(token)) {
                        validTokenFound = true;
                    } else {
                        // 令牌过期，处理过期情况
                        handleExpiredToken(response);
                        return false;
                    }
                }
            }
        }


        if (!validTokenFound) {
            // 未找到有效令牌，处理未登录情况
            handleUnauthorized(response);
            return false;
        }


        return true;
    }


    private void handleExpiredToken(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Token has expired. Please log in again.\"}");
    }


    private void handleUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Unauthorized. Please log in.\"}");
    }

}