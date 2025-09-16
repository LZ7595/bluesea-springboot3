package com.example.backend.Utils;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest; // 使用jakarta包
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class Jwt {

    // 从配置文件读取密钥和有效期
    @Value("${jwt.access.secret}")
    private String accessSecretKey;

    @Value("${jwt.refresh.secret}")
    private String refreshSecretKey;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpirationTime;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpirationTime;

    // 生成访问令牌
    public String generateAccessToken(int id, String username, String role, String avatar) {
        return generateToken(id, username, role, avatar, accessTokenExpirationTime, accessSecretKey);
    }

    // 生成刷新令牌
    public String generateRefreshToken(int id, String username, String role, String avatar) {
        return generateToken(id, username, role, avatar, refreshTokenExpirationTime, refreshSecretKey);
    }

    private String generateToken(int id, String username, String role, String avatar, long expirationTime, String secretKey) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("avatar", avatar);
        claims.put("type", role.equals("admin") ? "admin" : "user"); // 添加令牌类型

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // 验证访问令牌
    public boolean validateAccessToken(String token) {
        return validateToken(token, accessSecretKey);
    }

    // 验证刷新令牌
    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshSecretKey);
    }

    private boolean validateToken(String token, String secretKey) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 从令牌中获取用户ID
    public Integer getIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("id", Integer.class);
    }

    // 从令牌中获取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    // 从令牌中获取角色
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    // 从令牌中获取头像
    public String getAvatarFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("avatar", String.class);
    }

    // 解析令牌
    private Claims parseToken(String token) {
        // 获取当前请求的HttpServletRequest（使用jakarta包）
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // 根据令牌来源判断使用哪个密钥
        String secretKey = determineSecretKey(token, request);

        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }

    // 根据令牌来源判断使用哪个密钥
    private String determineSecretKey(String token, HttpServletRequest request) {
        String accessTokenFromCookie = getCookieValue(request, "accessToken");
        String refreshTokenFromCookie = getCookieValue(request, "refreshToken");

        if (accessTokenFromCookie != null && accessTokenFromCookie.equals(token)) {
            return accessSecretKey;
        } else if (refreshTokenFromCookie != null && refreshTokenFromCookie.equals(token)) {
            return refreshSecretKey;
        } else {
            // 尝试根据令牌类型字段判断
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(accessSecretKey)
                        .parseClaimsJws(token)
                        .getBody();

                // 检查令牌类型
                String type = claims.get("type", String.class);
                if ("admin".equals(type) || "user".equals(type)) {
                    return accessSecretKey;
                }
            } catch (JwtException e) {
                // 不是访问令牌，尝试刷新令牌密钥
                try {
                    Jwts.parser()
                            .setSigningKey(refreshSecretKey)
                            .parseClaimsJws(token);
                    return refreshSecretKey;
                } catch (JwtException ex) {
                    throw new IllegalArgumentException("无效的令牌");
                }
            }

            throw new IllegalArgumentException("无法确定令牌类型");
        }
    }

    // 辅助方法：从Cookie获取令牌
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 判断令牌是否即将过期
     * @param token 令牌
     * @param threshold 阈值（剩余时间占比，如0.3表示剩余30%有效期时认为即将过期）
     * @return 是否即将过期
     */
    public boolean isTokenAboutToExpire(String token, double threshold) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();

        // 计算总有效期（毫秒）
        long totalValidity = expiration.getTime() - claims.getIssuedAt().getTime();
        // 计算剩余有效期（毫秒）
        long remainingValidity = expiration.getTime() - now.getTime();

        // 如果剩余有效期小于总有效期的阈值比例，则认为即将过期
        return (double) remainingValidity / totalValidity < threshold;
    }

    /**
     * 获取令牌的剩余过期时间
     * @param token 令牌
     * @return 剩余过期时间（毫秒），如果令牌已过期返回负数
     */
    public long getRemainingTime(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();

        return expiration.getTime() - now.getTime();
    }
}