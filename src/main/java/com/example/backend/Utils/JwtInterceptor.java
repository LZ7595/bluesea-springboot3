package com.example.backend.Utils;

import com.example.backend.Service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // -------------------------- 1. 定义客户端类型常量（与前端一致）--------------------------
    // 对应前端 Client-Type 的值，确保完全匹配
    private static final String CLIENT_TYPE_HEADER = "Client-Type"; // 前端传递的客户端类型头
    private static final String CLIENT_APP = "app";                 // App端（UniApp打包的App）
    private static final String CLIENT_H5 = "h5";                  // H5端
    private static final String CLIENT_MINIPROGRAM = "miniprogram"; // 微信小程序端


    // -------------------------- 2. 构造函数与依赖注入（保持原有）--------------------------
    public JwtInterceptor() {
    }

    public JwtInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

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


    // -------------------------- 3. 核心：按客户端类型提取 Token --------------------------
    /**
     * 根据客户端类型提取 AccessToken（适配前端不同传递方式）
     * - App端：从 Cookie 提取（前端将 fullCookie 放入 Cookie 头）
     * - H5端：从 Cookie 提取（浏览器自动携带 HttpOnly Cookie）
     * - 小程序端：从 Authorization 头提取（格式：Bearer accessToken）
     */
    private String extractAccessTokenByClient(HttpServletRequest request) {
        // 1. 先获取客户端类型（默认按H5处理，避免空值）
        String clientType = request.getHeader(CLIENT_TYPE_HEADER);
        if (clientType == null || clientType.trim().isEmpty()) {
            logger.warn("未传递 Client-Type，默认按 H5 处理");
            clientType = CLIENT_H5;
        }

        // 2. 按客户端类型提取 AccessToken
        switch (clientType) {
            case CLIENT_APP:
            case CLIENT_H5:
                // App/H5：从 Cookie 提取 "accessToken"（前端传递的 fullCookie 包含 accessToken=xxx）
                return getCookieValue(request, "accessToken");

            case CLIENT_MINIPROGRAM:
                // 小程序：从 Authorization 头提取（格式：Bearer accessTokenValue）
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // 截取 "Bearer " 后面的 Token 部分（前端逻辑：refreshToken.split('=')[1] 取纯值）
                    return authHeader.substring(7).trim();
                }
                logger.warn("小程序端 Authorization 头格式错误，应为 Bearer {accessToken}");
                return null;

            default:
                logger.error("未知客户端类型：{}，无法提取 AccessToken", clientType);
                return null;
        }
    }

    /**
     * 根据客户端类型提取 RefreshToken（适配前端不同传递方式）
     * - App端：从 Cookie 提取（前端将 refreshTokenCookie 放入 Cookie 头）
     * - H5端：从 Cookie 提取（浏览器自动携带 HttpOnly Cookie）
     * - 小程序端：从 Authorization 头提取（前端传递的是 refreshToken=xxx 的值，需截取）
     */
    private String extractRefreshTokenByClient(HttpServletRequest request) {
        String clientType = request.getHeader(CLIENT_TYPE_HEADER);
        if (clientType == null || clientType.trim().isEmpty()) {
            clientType = CLIENT_H5;
        }

        switch (clientType) {
            case CLIENT_APP:
            case CLIENT_H5:
                // App/H5：从 Cookie 提取 "refreshToken"
                return getCookieValue(request, "refreshToken");

            case CLIENT_MINIPROGRAM:
                // 小程序：从 refreshToken 头提取（前端传递的是 "refreshToken=xxx" 格式，需截取值）
                String authHeader = request.getHeader("refreshToken");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String refreshTokenWithKey = authHeader.substring(7).trim();
                        return refreshTokenWithKey;
                }
                logger.warn("小程序端 RefreshToken 格式错误，应为 Bearer refreshToken=xxx");
                return null;

            default:
                logger.error("未知客户端类型：{}，无法提取 RefreshToken", clientType);
                return null;
        }
    }


    // -------------------------- 4. 辅助方法（Cookie提取、Token续约、未授权处理）--------------------------
    /**
     * 从请求Cookie中提取指定名称的值（原有方法保留）
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 根据客户端类型更新 AccessToken（适配前端接收方式）
     * - App/H5：通过 Set-Cookie 头返回（前端会同步到本地存储）
     * - 小程序：无需 Set-Cookie，前端会从响应体提取新 Token（若有）
     */
    private void updateAccessTokenByClient(HttpServletResponse response, HttpServletRequest request, String newAccessToken) {
        String clientType = request.getHeader(CLIENT_TYPE_HEADER);
        if (clientType == null) {
            clientType = CLIENT_H5;
        }

        // 仅 App/H5 需要通过 Cookie 更新 Token（小程序前端会自己处理响应体的 Token）
        if (CLIENT_APP.equals(clientType) || CLIENT_H5.equals(clientType)) {
            ResponseCookie cookie = ResponseCookie.from("accessToken", newAccessToken)
                    .httpOnly(true) // 禁止前端JS读取，防XSS
                    .maxAge(accessTokenExpirationTime / 1000) // 单位：秒（与前端过期时间一致）
                    .path("/") // 全站生效
                    // 生产环境建议添加：secure=true（仅HTTPS）、sameSite="Lax"（防CSRF）
                    // .secure(true)
                    // .sameSite("Lax")
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());
            logger.info("{} 端 AccessToken 已通过 Cookie 续约", clientType);
        } else if (CLIENT_MINIPROGRAM.equals(clientType)) {
            // 小程序：可通过响应头返回新 Token（前端会存储到 fullCookie）
            response.setHeader("X-New-AccessToken", newAccessToken);
            logger.info("小程序端 AccessToken 已通过 X-New-AccessToken 头返回");
        }
    }

    /**
     * 将 Token 中的用户信息存入请求属性（原有方法保留）
     */
    private void setUserAttributes(HttpServletRequest request, String token) {
        request.setAttribute("userId", jwt.getIdFromToken(token));
        request.setAttribute("username", jwt.getUsernameFromToken(token));
        request.setAttribute("role", jwt.getRoleFromToken(token));
    }

    /**
     * 处理未授权请求（原有方法保留，确保响应格式与前端一致）
     */
    private void handleUnauthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            Map<String, String> result = new HashMap<>();
            result.put("code", "401");
            result.put("message", "未授权，请登录"); // 前端会弹窗显示此消息
            PrintWriter writer = response.getWriter();
            writer.write(new ObjectMapper().writeValueAsString(result));
            writer.flush();
        } catch (IOException e) {
            logger.error("处理未授权请求时发生错误", e);
        }
    }


    // -------------------------- 5. 拦截器核心逻辑（preHandle）--------------------------
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestMethod = request.getMethod(); // GET/POST等
        String requestPath = request.getRequestURI(); // 请求路径（如：/categoryBrand/brandList）
        String clientIp = request.getRemoteAddr(); // 客户端IP

        // 打印日志（用info级别，方便查看）
        logger.info("【JWT拦截器】收到请求 -> 方法: {}, 路径: {}, 客户端IP: {}",
                requestMethod, requestPath, clientIp);

        // 1. 按客户端类型提取 AccessToken 和 RefreshToken
        String accessToken = extractAccessTokenByClient(request);
        String refreshToken = extractRefreshTokenByClient(request);
        String clientType = request.getHeader(CLIENT_TYPE_HEADER);
        logger.info("客户端类型：{}，AccessToken存在：{}，RefreshToken存在：{}",
                clientType, accessToken != null, refreshToken != null);

        // 2. 验证 AccessToken 有效性（有效则直接放行，顺便处理续约）
        if (accessToken != null && jwt.validateAccessToken(accessToken)) {
            setUserAttributes(request, accessToken); // 存入用户信息，供后续接口使用

            // 检查 AccessToken 是否即将过期（触发自动续约，仅 App/H5 需处理）
            if (jwt.isTokenAboutToExpire(accessToken, RENEW_THRESHOLD)) {
                try {
                    if (refreshToken != null && jwt.validateRefreshToken(refreshToken)) {
                        String newAccessToken = authService.refreshToken(refreshToken);
                        if (newAccessToken != null) {
                            updateAccessTokenByClient(response, request, newAccessToken); // 按客户端类型更新Token
                        }
                    }
                } catch (Exception e) {
                    logger.error("{} 端 AccessToken 续约失败", clientType, e);
                    // 续约失败不影响本次请求（用户仍可正常使用当前Token）
                }
            }
            return true; // Token 有效，放行请求
        }

        // 3. AccessToken 无效，尝试用 RefreshToken 刷新
        if (refreshToken != null && jwt.validateRefreshToken(refreshToken)) {
            try {
                String newAccessToken = authService.refreshToken(refreshToken);
                if (newAccessToken != null) {
                    updateAccessTokenByClient(response, request, newAccessToken); // 返回新Token给前端
                    setUserAttributes(request, newAccessToken); // 存入新的用户信息
                    return true; // 刷新成功，放行请求
                }
            } catch (Exception e) {
                logger.error("{} 端 RefreshToken 刷新失败", clientType, e);
            }
        }

        // 4. 所有验证失败，返回未授权
        handleUnauthorized(response);
        return false;
    }
}