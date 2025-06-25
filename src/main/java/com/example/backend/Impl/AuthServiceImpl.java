package com.example.backend.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.example.backend.Dao.AuthMapper;
import com.example.backend.Entity.Enum.LoginType;
import com.example.backend.Entity.Enum.ErrorType;
import com.example.backend.Entity.ErrorData;
import com.example.backend.Entity.User;
import com.example.backend.Service.AuthService;
import com.example.backend.Utils.Email;
import com.example.backend.Utils.Encryption;
import com.example.backend.Utils.Jwt;
import com.example.backend.Utils.Validation;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.example.backend.Utils.Jwt.*;
import static com.example.backend.Utils.RedisConstants.*;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private AuthMapper authMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpirationTime;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpirationTime;

    @Autowired
    private Email emailsend;

    @Resource
    private Jwt jwt;

    @Override
    public ResponseEntity<?> sendVerificationCode(String email, int type) {

        // 验证邮箱地址是否正确
        if (!Validation.isValidEmail(email)) {
            return ResponseEntity.status(500).body(ErrorType.EMAIL_VERIFICATION_FAILED.toErrorResponse());
        }

        try {
            String code = generateCode();
            System.out.println(email + code);
            emailsend.sendEmail(email, code, "登录注册");
            stringRedisTemplate.opsForValue().set(AUTH_CODE_KEY + email, code, CODE_EXPIRE_TIME, TimeUnit.SECONDS);
            return ResponseEntity.ok("验证码已发送");
        } catch (Exception e) {
            // 更详细的异常日志记录
            System.err.println("发送验证码时发生异常: " + e.getMessage());
            return ResponseEntity.status(500).body(ErrorType.CODE_SENDING_FAILED.toErrorResponse());
        }
    }

    @Override
    public boolean validateCode(String email, String code) {
        String codeInRedis = stringRedisTemplate.opsForValue().get(AUTH_CODE_KEY + email);
        if (codeInRedis == null || !codeInRedis.equals(code)) { // 验证码不存在或验证码不匹配
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isUsernameUsed(String username) {
        int result = authMapper.selectUsername(username);
        System.out.println(result);
        if (result == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isEmailUsed(String email) {
        int result = authMapper.selectEmail(email);
        if (result == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public ResponseEntity<?> registerUser(User user) {

        // 如果邮箱地址不为空，验证邮箱地址是否正确
        if (user.getEmail() != null && !Validation.isValidEmail(user.getEmail())) {
            return ResponseEntity.status(500).body(ErrorType.EMAIL_VERIFICATION_FAILED.toErrorResponse());
        }

        // 检查用户名和邮箱是否已被注册
        if (isUsernameUsed(user.getUsername())) {
            return ResponseEntity.status(409).body(ErrorType.USERNAME_REGISTERED.toErrorResponse()); // 用户名已注册过
        }

        if (isEmailUsed(user.getEmail())) {
            return ResponseEntity.status(409).body(ErrorType.EMAIL_REGISTERED.toErrorResponse()); // 邮箱已注册过
        }
        // 验证验证码
        if (!validateCode(user.getEmail(), user.getCode())) {
            return ResponseEntity.status(401).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse()); // 验证码错误
        }

        user.setPassword(Encryption.encryptPassword(user.getPassword()));
        System.out.println(user.getPassword());
        Boolean res = authMapper.insert(user);
        if (res) {
            return ResponseEntity.ok("注册成功");
        } else {
            return ResponseEntity.status(500).body(ErrorType.REGISTER_FAILED.toErrorResponse());
        }
    }

    @Override
    public ResponseEntity<?> loginUser(User user, LoginType type) {
        // 通用参数校验
        if (type == null) {
            return ResponseEntity.badRequest().body(ErrorType.LOGIN_TYPE_INVALID.toErrorResponse());
        }

        User authenticatedUser = null;
        String loginIdentifier = null;
        String loginMethod = null;

        try {
            // 根据登录类型获取用户信息
            switch (type) {
                case USER_PASSWORD:
                    loginIdentifier = user.getUsername();
                    loginMethod = "用户密码";
                    authenticatedUser = authenticateByUsernamePassword(user);
                    break;

                case EMAIL_VERIFICATION:
                    loginIdentifier = user.getEmail();
                    loginMethod = "邮箱验证";
                    validateEmailFormat(user.getEmail());
                    authenticatedUser = authenticateByEmailCode(user);
                    break;

                case EMAIL_PASSWORD:
                    loginIdentifier = user.getEmail();
                    loginMethod = "邮箱密码";
                    validateEmailFormat(user.getEmail());
                    authenticatedUser = authenticateByEmailPassword(user);
                    break;

                default:
                    return ResponseEntity.status(400).body(ErrorType.LOGIN_FAILED.toErrorResponse());
            }

            // 用户验证成功，生成双令牌并存储到 Redis
            Map<String, String> tokens = generateAndStoreTokens(authenticatedUser);
            String accessToken = tokens.get("accessToken");
            String refreshToken = tokens.get("refreshToken");

            // 更新登录时间
            authMapper.updateLastLoginTime(loginIdentifier, LocalDateTime.now(), loginMethod);

            // 设置响应 Cookie（包含双令牌）
            HttpHeaders headers = createTokenCookies(accessToken, refreshToken);

            // 返回双令牌
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("expiresIn", accessTokenExpirationTime / 1000); // 访问令牌过期时间（秒）

            return ResponseEntity.ok().headers(headers).body(response);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(e.getErrorResponse());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 用户名密码验证
    private User authenticateByUsernamePassword(User user) throws AuthenticationException {
        User result = authMapper.LoginVerification(user.getUsername());
        if (result == null) {
            throw new AuthenticationException(404, ErrorType.USERNAME_ERROR);
        }
        if (!Encryption.verifyPassword(user.getPassword(), result.getPassword())) {
            throw new AuthenticationException(401, ErrorType.PASSWORD_ERROR);
        }
        result.setAvatar(authMapper.getImageUrlsByUserId(result.getId()));
        return result;
    }

    // 邮箱验证码验证
    private User authenticateByEmailCode(User user) throws AuthenticationException {
        if (!isEmailUsed(user.getEmail())) {
            throw new AuthenticationException(404, ErrorType.EMAIL_NOT_REGISTERED);
        }
        if (!validateCode(user.getEmail(), user.getCode())) {
            throw new AuthenticationException(401, ErrorType.CODE_INVALID_FAILED);
        }

        User result = authMapper.LoginVerification(user.getEmail());
        result.setAvatar(authMapper.getImageUrlsByUserId(result.getId()));
        return result;
    }

    // 邮箱密码验证
    private User authenticateByEmailPassword(User user) throws AuthenticationException {
        if (!isEmailUsed(user.getEmail())) {
            throw new AuthenticationException(404, ErrorType.EMAIL_NOT_REGISTERED);
        }
        User result = authMapper.LoginVerification(user.getEmail());
        if (result == null) {
            throw new AuthenticationException(404, ErrorType.EMAIL_NOT_REGISTERED);
        }
        if (!Encryption.verifyPassword(user.getPassword(), result.getPassword())) {
            throw new AuthenticationException(401, ErrorType.PASSWORD_ERROR);
        }
        result.setAvatar(authMapper.getImageUrlsByUserId(result.getId()));
        return result;
    }

    // 生成 JWT 并存储到 Redis

    /**
     * 生成 JWT 并将用户信息存储到 Redis（使用哈希结构）
     */
    // 生成双令牌并存储到 Redis
    private Map<String, String> generateAndStoreTokens(User user) {
        // 生成访问令牌（1小时）和刷新令牌（7天）
        String accessToken = jwt.generateAccessToken(user.getId(), user.getUsername(),
                user.getRole().toString(), user.getAvatar());
        String refreshToken = jwt.generateRefreshToken(user.getId(), user.getUsername(),
                user.getRole().toString(), user.getAvatar());

        // 存储用户信息到 Redis（使用访问令牌作为键）
        String userRedisKey = AUTH_USER_KEY + accessToken;
        Map<String, Object> userHash = new HashMap<>();
        userHash.put("id", user.getId().toString());
        userHash.put("username", user.getUsername());
        userHash.put("role", user.getRole().toString());
        userHash.put("avatar", user.getAvatar());
        Map<String, Object> userMap = BeanUtil.beanToMap(userHash);
        stringRedisTemplate.opsForHash().putAll(userRedisKey, userMap);
        stringRedisTemplate.expire(userRedisKey, accessTokenExpirationTime, TimeUnit.MILLISECONDS);

        // 存储刷新令牌到 Redis
        stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY + refreshToken,
                user.getId().toString(),
                refreshTokenExpirationTime,
                TimeUnit.MILLISECONDS
        );

        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    // 创建包含双令牌的 Cookie
    private HttpHeaders createTokenCookies(String accessToken, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();

        // HttpOnly 的访问令牌 Cookie
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .maxAge(accessTokenExpirationTime / 1000)
                .path("/")
                .build();

        // 刷新令牌 Cookie（建议通过 Header 传递，而非 Cookie）
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .maxAge(refreshTokenExpirationTime / 1000)
                .path("/")
                .build();

        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return headers;
    }

    // 验证邮箱格式
    private void validateEmailFormat(String email) {
        if (email == null || !Validation.isValidEmail(email)) {
            throw new IllegalArgumentException("邮箱地址不正确");
        }
    }

    // 自定义认证异常类
    private static class AuthenticationException extends Exception {
        private final int httpStatus;
        private final ErrorData errorResponse;

        public AuthenticationException(int httpStatus, ErrorType errorType) {
            this.httpStatus = httpStatus;
            this.errorResponse = errorType.toErrorResponse();
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public ErrorData getErrorResponse() {
            return errorResponse;
        }
    }

    @Override
    public ResponseEntity<?> logoutUser() {
        // 从请求中获取刷新令牌（实际应用中需从请求中提取）
        String refreshToken = "从请求中获取的刷新令牌"; // 实际需从请求解析

        // 清除 Redis 中的刷新令牌
        if (refreshToken != null) {
            stringRedisTemplate.delete(REFRESH_TOKEN_KEY + refreshToken);
        }

        // 清除 Cookie
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, "accessToken=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; HttpOnly");
        headers.add(HttpHeaders.SET_COOKIE, "refreshToken=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; HttpOnly");

        return ResponseEntity.ok().headers(headers).body("登出成功");
    }


    @Override
    public String refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!jwt.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("无效的刷新令牌");
        }

        // 从Redis检查刷新令牌是否存在
        String userIdStr = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + refreshToken);
        if (userIdStr == null) {
            throw new IllegalArgumentException("刷新令牌已过期");
        }

        // 获取用户信息
        Integer userId = Integer.parseInt(userIdStr);
        User user = authMapper.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 生成新的访问令牌
        String newAccessToken = jwt.generateAccessToken(user.getId(), user.getUsername(),
                user.getRole().toString(), user.getAvatar());

        // 更新Redis中的用户信息
        String userRedisKey = AUTH_USER_KEY + newAccessToken;
        Map<String, Object> userHash = new HashMap<>();
        userHash.put("id", user.getId().toString());
        userHash.put("username", user.getUsername());
        userHash.put("role", user.getRole().toString());
        userHash.put("avatar", user.getAvatar());
        stringRedisTemplate.opsForHash().putAll(userRedisKey, userHash);
        stringRedisTemplate.expire(userRedisKey, accessTokenExpirationTime, TimeUnit.MILLISECONDS);

        // 续约刷新令牌（可选）
        // 这里可以选择每次刷新时生成新的刷新令牌，提高安全性
        // 或者只是延长现有刷新令牌的有效期
        stringRedisTemplate.expire(
                REFRESH_TOKEN_KEY + refreshToken,
                refreshTokenExpirationTime,
                TimeUnit.MILLISECONDS
        );

        return newAccessToken;
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
