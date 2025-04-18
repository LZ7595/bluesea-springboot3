package com.example.backend.Utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Component
public class Jwt {
    // 密钥，应存储在更安全的地方，如环境变量或配置文件中
    private static final String SECRET_KEY = "BlueSeaTechnologyUsedDigitalMallJwtToken123456";
    // 刷新令牌有效期，这里设置为 24 小时
    public static final long TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    // 生成刷新令牌
    public static String generateRefreshToken(int id,String username, String role, String avatar) {
        return generateToken(id, username, role, avatar, TOKEN_EXPIRATION_TIME);
    }


    private static String generateToken(int id,String username, String role, String avatar, long expirationTime) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);
        claims.put("id",id);
        claims.put("avatar",avatar);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }


    // 从令牌中获取用户名的方法
    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }


    // 验证 JWT 令牌的方法
    public static boolean validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            // 检查令牌是否过期
            Date expiration = claims.getExpiration();
            return !expiration.before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    // 从令牌中获取角色信息
    public static String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
        return (String) claims.get("role");
    }

    public static Integer validateToken(String token) {
        try {
            return Integer.parseInt(Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody().getSubject());
        } catch (Exception e) {
            return null;
        }
    }

}