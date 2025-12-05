package com.example.backend.Utils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Common {

    @Resource
    private Cookie cookie;

    /**
     * 增强的accessToken提取方法，支持多种头部格式
     */
    public String extractAccessTokenByClient(HttpServletRequest request) {
        try {
            String clientType = request.getHeader("Client-Type");
            if (clientType == null) {
                clientType = "h5"; // 默认按H5处理
                log.warn("未传递Client-Type，默认按H5处理");
            }

            log.info("提取accessToken - 客户端类型: {}, 请求路径: {}", clientType, request.getRequestURI());

            String accessToken = null;

            switch (clientType) {
                case "app":
                case "h5":
                    // App/H5从Cookie提取
                    accessToken = cookie.getCookieValue(request, "accessToken");
                    log.debug("App/H5端从Cookie提取accessToken: {}", accessToken != null ? "存在" : "为空");
                    break;

                case "miniprogram":
                    // 小程序：优先尝试accessToken头，然后尝试accessToken头
                    accessToken = extractFromAccessTokenHeader(request);
                    if (accessToken == null) {
                        accessToken = extractFromAuthorizationHeader(request);
                    }
                    log.debug("小程序端提取accessToken: {}", accessToken != null ? "存在" : "为空");
                    break;

                default:
                    log.error("未知客户端类型: {}", clientType);
                    return null;
            }

            log.info("最终提取的accessToken - 客户端: {}, 长度: {}", clientType, accessToken != null ? accessToken.length() : 0);
            return accessToken;

        } catch (Exception e) {
            log.error("提取accessToken时发生异常", e);
            return null;
        }
    }

    /**
     * 从accessToken头提取（处理小写accessToken头）
     */
    private String extractFromAccessTokenHeader(HttpServletRequest request) {
        // 尝试所有可能的accessToken头名称
        String[] possibleHeaders = {"accessToken", "accesstoken", "Access-Token", "X-Access-Token"};

        for (String headerName : possibleHeaders) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && !headerValue.trim().isEmpty()) {
                log.debug("找到头 {}: {}", headerName, headerValue);
                if (headerValue.startsWith("Bearer ")) {
                    return headerValue.substring(7).trim();
                }
                return headerValue.trim(); // 直接返回token值
            }
        }
        return null;
    }

    /**
     * 从标准accessToken头提取
     */
    private String extractFromAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("accessToken");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("从accessToken头提取到accessToken");
            return authHeader.substring(7).trim();
        }
        return null;
    }

    /**
     * 调试方法：打印所有请求头信息
     */
    public void debugRequestHeaders(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== 请求头详情 ===\n");
        sb.append("方法: ").append(request.getMethod()).append("\n");
        sb.append("URL: ").append(request.getRequestURL()).append("\n");
        sb.append("Client-Type: ").append(request.getHeader("Client-Type")).append("\n");

        // 打印所有相关头信息
        String[] relevantHeaders = {
                "Authorization", "authorization",
                "accessToken", "accesstoken", "Access-Token", "X-Access-Token",
                "refreshToken", "refreshtoken", "Refresh-Token", "X-Refresh-Token"
        };

        for (String header : relevantHeaders) {
            String value = request.getHeader(header);
            if (value != null) {
                sb.append(header).append(": ").append(value).append("\n");
            }
        }

        log.info(sb.toString());
    }
}