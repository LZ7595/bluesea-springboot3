package com.example.backend.Impl;

import com.example.backend.Dao.AuthMapper;
import com.example.backend.Entity.Enum.LoginType;
import com.example.backend.Entity.Enum.ErrorType;
import com.example.backend.Entity.User;
import com.example.backend.Service.AuthService;
import com.example.backend.Utils.Email;
import com.example.backend.Utils.Encryption;
import com.example.backend.Utils.Jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private Email emailsend;
    private final Map<String, String> verificationCodes = new HashMap<>();
    private final Map<String, LocalDateTime> codeExpiration = new HashMap<>();

    private static final long CODE_EXPIRATION_TIME = 5 * 60 * 1000; // 验证码过期时间，5分钟

    @Override
    public ResponseEntity<?> sendVerificationCode(String email, int type) {
        String code = generateCode();
        verificationCodes.put(email, code);
        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(CODE_EXPIRATION_TIME / 1000);
        codeExpiration.put(email, expirationTime);
        System.out.println(email + code);
        emailsend.sendEmail(email, code, "登录注册");
        try {
            if (type == 1) {
                ResponseEntity<?> storeResult = storeCodeInDatabase(email, code, expirationTime);
                if (storeResult.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                    return storeResult;
                }
            }
            return ResponseEntity.ok("验证码已发送");
        } catch (Exception e) {
            // 更详细的异常日志记录
            System.err.println("发送验证码时发生异常: " + e.getMessage());
            return ResponseEntity.status(500).body(ErrorType.CODE_SENDING_FAILED.toErrorResponse());
        }
    }

    private ResponseEntity<?> storeCodeInDatabase(String email, String code, LocalDateTime expirationTime) {
        // 先尝试更新已存在的记录
        System.out.println(code);
        System.out.println(expirationTime);
        try {
            int rowsUpdated = authMapper.updateCode(code, expirationTime, email);
            System.out.println(rowsUpdated);
            if (rowsUpdated == 0) {
                return ResponseEntity.status(404)
                        .body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
            }
        } catch (Exception e) {
            // 更详细的异常日志记录
            System.err.println("存储验证码时发生异常: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(ErrorType.CODE_INSERT_FAILED.toErrorResponse());
        }
        return ResponseEntity.ok("验证码已存储");
    }

    @Override
    public boolean validateCode(String email, String code) {
        LocalDateTime expiration = codeExpiration.get(email);
        if (expiration == null || expiration.isBefore(LocalDateTime.now())) {
            return false;
        }
        return verificationCodes.get(email).equals(code);
    }

    @Override
    public boolean isUsernameUsed(String username) {
        Boolean result = authMapper.selectUsernameOne(username);
        return result != null && result;
    }

    @Override
    public boolean isEmailUsed(String email) {
        int result = authMapper.selectEmailOne(email);
        if (result == 0) {
            return false;
        }else {
            return true;
        }
    }

    @Override
    public ResponseEntity<?> registerUser(User user) {
        user.setPassword(Encryption.encryptPassword(user.getPassword()));
        System.out.println(user.getPassword());
        authMapper.insert(user);
        return ResponseEntity.ok("注册成功");
    }

    @Override
    public ResponseEntity<?> loginUser(User user, LoginType type) {
        switch (type) {
            case USER_PASSWORD -> {
                User result = authMapper.LoginVerification(user.getUsername());
                if (result != null) {
                    if (Encryption.verifyPassword(user.getPassword(), result.getPassword())) {
                        authMapper.updateLastLoginTime(user.getUsername(), LocalDateTime.now(), "用户密码");
                        String token = Jwt.generateRefreshToken(result.getId(),user.getUsername(), result.getRole().toString());
                        // 创建 HttpOnly 的 Cookie
                        ResponseCookie cookie1 = createHttpOnlyCookie(token);
                        ResponseCookie cookie2 = createCookie(token);
                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.SET_COOKIE, cookie1.toString());
                        headers.add(HttpHeaders.SET_COOKIE, cookie2.toString());
                        return ResponseEntity.ok()
                                .headers(headers)
                                .body(token);
                    } else {
                        return ResponseEntity.status(401)
                                .body(ErrorType.PASSWORD_ERROR.toErrorResponse());
                    }
                } else {
                    return ResponseEntity.status(404)
                            .body(ErrorType.USERNAME_ERROR.toErrorResponse());
                }
            }
            case EMAIL_VERIFICATION -> {
                // 这里需要添加验证码登录的逻辑
                if (isEmailUsed(user.getEmail())) {
                    if (validateCode(user.getEmail(), user.getCode())) {
                        // 登录成功，清除验证码和过期时间
                        clearCodeFromDatabase(user.getEmail());
                        User result1 = authMapper.LoginVerification(user.getEmail());
                        authMapper.updateLastLoginTime(user.getEmail(), LocalDateTime.now(), "邮箱验证");
                        String token = Jwt.generateRefreshToken(result1.getId(),result1.getUsername(), result1.getRole().toString());
                        // 创建 HttpOnly 的 Cookie
                        ResponseCookie cookie1 = createHttpOnlyCookie(token);
                        ResponseCookie cookie2 = createCookie(token);
                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.SET_COOKIE, cookie1.toString());
                        headers.add(HttpHeaders.SET_COOKIE, cookie2.toString());
                        return ResponseEntity.ok()
                                .headers(headers)
                                .body(token);
                    } else {
                        return ResponseEntity.status(401)
                                .body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
                    }
                } else {
                    return ResponseEntity.status(404)
                            .body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
                }
            }
            case EMAIL_PASSWORD -> {
                if (isEmailUsed(user.getEmail())) {
                    User result2 = authMapper.LoginVerification(user.getEmail());
                    if (result2 != null) {
                        if (Encryption.verifyPassword(user.getPassword(), result2.getPassword())) {
                            authMapper.updateLastLoginTime(user.getEmail(), LocalDateTime.now(), "邮箱密码");
                            String token = Jwt.generateRefreshToken(result2.getId(),result2.getUsername(), result2.getRole().toString());
                            // 创建 HttpOnly 的 Cookie
                            ResponseCookie cookie1 = createHttpOnlyCookie(token);
                            ResponseCookie cookie2 = createCookie(token);
                            HttpHeaders headers = new HttpHeaders();
                            headers.add(HttpHeaders.SET_COOKIE, cookie1.toString());
                            headers.add(HttpHeaders.SET_COOKIE, cookie2.toString());
                            return ResponseEntity.ok()
                                    .headers(headers)
                                    .body(token);
                        } else {
                            return ResponseEntity.status(401)
                                    .body(ErrorType.PASSWORD_ERROR.toErrorResponse());
                        }
                    } else {
                        return ResponseEntity.status(404)
                                .body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
                    }
                } else {
                    return ResponseEntity.status(404)
                            .body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
                }
            }
            default -> {
                System.out.println("登录失败");
                return ResponseEntity.status(400)
                        .body(ErrorType.LOGIN_FAILED.toErrorResponse());
            }
        }
    }

    private ResponseCookie createHttpOnlyCookie(String token) {
        return ResponseCookie.from("jwtToken", token)
                .httpOnly(true)
                .maxAge(Jwt.TOKEN_EXPIRATION_TIME / 1000) // 转换为秒
                .path("/")
                .build();
    }
    private ResponseCookie createCookie(String token) {
        return ResponseCookie.from("token", token)
                .httpOnly(false)
                .maxAge(Jwt.TOKEN_EXPIRATION_TIME / 1000) // 转换为秒
                .path("/")
                .build();
    }


    // 登出时清除刷新令牌
    public ResponseEntity<?> logoutUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, "jwtToken=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; HttpOnly");
        headers.add(HttpHeaders.SET_COOKIE, "token=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/;");
        return ResponseEntity.ok().headers(headers).body("登出成功");
    }

    private void clearCodeFromDatabase(String email) {
        authMapper.clearCode(email);
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // 定期清理过期验证码
    @Scheduled(fixedRate = 60 * 1000) // 每分钟检查一次
    public void clearExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        codeExpiration.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        verificationCodes.keySet().removeIf(key -> codeExpiration.get(key) == null);
        // 清除数据库中的过期验证码
        authMapper.deleteExpiredCodes(now);
    }
}
