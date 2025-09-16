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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static com.example.backend.Utils.RedisConstants.*;

@Service
public class AuthServiceImpl implements AuthService {
    // 日志实例
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    // 依赖注入
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


    // 发送验证码
    @Override
    public ResponseEntity<?> sendVerificationCode(String email, int type) {
        if (!Validation.isValidEmail(email)) {
            logger.warn("发送验证码失败：邮箱格式无效，email: {}", email);
            return ResponseEntity.status(500).body(ErrorType.EMAIL_VERIFICATION_FAILED.toErrorResponse());
        }

        try {
            String code = generateCode();
            logger.debug("生成验证码：{}，发送至邮箱：{}", code, email);

            emailsend.sendEmail(email, code, "登录注册");
            stringRedisTemplate.opsForValue().set(
                    AUTH_CODE_KEY + email,
                    code,
                    CODE_EXPIRE_TIME,
                    TimeUnit.SECONDS
            );

            logger.info("验证码发送成功，email: {}", email);
            return ResponseEntity.ok("验证码已发送");
        } catch (Exception e) {
            logger.error("发送验证码异常，email: {}", email, e);
            return ResponseEntity.status(500).body(ErrorType.CODE_SENDING_FAILED.toErrorResponse());
        }
    }


    // 验证码校验
    @Override
    public boolean validateCode(String email, String code) {
        String codeInRedis = stringRedisTemplate.opsForValue().get(AUTH_CODE_KEY + email);
        boolean isValid = codeInRedis != null && codeInRedis.equals(code);

        if (!isValid) {
            logger.warn("验证码校验失败，email: {}, 输入code: {}, Redis code: {}",
                    email, code, codeInRedis);
        }
        return isValid;
    }


    // 用户名占用校验
    @Override
    public boolean isUsernameUsed(String username) {
        int result = authMapper.selectUsername(username);
        boolean isUsed = result != 0;
        logger.debug("用户名占用校验：username: {}, 状态: {}", username, isUsed);
        return isUsed;
    }

    // 邮箱占用校验
    @Override
    public boolean isEmailUsed(String email) {
        int result = authMapper.selectEmail(email);
        boolean isUsed = result != 0;
        logger.debug("邮箱占用校验：email: {}, 状态: {}", email, isUsed);
        return isUsed;
    }


    // 用户注册
    @Override
    public ResponseEntity<?> registerUser(User user) {
        if (user.getEmail() != null && !Validation.isValidEmail(user.getEmail())) {
            logger.warn("注册失败：邮箱格式无效，email: {}", user.getEmail());
            return ResponseEntity.status(500).body(ErrorType.EMAIL_VERIFICATION_FAILED.toErrorResponse());
        }

        if (isUsernameUsed(user.getUsername())) {
            logger.warn("注册失败：用户名已占用，username: {}", user.getUsername());
            return ResponseEntity.status(409).body(ErrorType.USERNAME_REGISTERED.toErrorResponse());
        }

        if (isEmailUsed(user.getEmail())) {
            logger.warn("注册失败：邮箱已占用，email: {}", user.getEmail());
            return ResponseEntity.status(409).body(ErrorType.EMAIL_REGISTERED.toErrorResponse());
        }

        if (!validateCode(user.getEmail(), user.getCode())) {
            logger.warn("注册失败：验证码无效，email: {}", user.getEmail());
            return ResponseEntity.status(401).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
        }

        try {
            user.setPassword(Encryption.encryptPassword(user.getPassword()));
            Boolean res = authMapper.insert(user);

            if (res) {
                logger.info("注册成功：username: {}, email: {}", user.getUsername(), user.getEmail());
                return ResponseEntity.ok("注册成功");
            } else {
                logger.error("注册失败：数据库插入失败，username: {}", user.getUsername());
                return ResponseEntity.status(500).body(ErrorType.REGISTER_FAILED.toErrorResponse());
            }
        } catch (Exception e) {
            logger.error("注册异常，username: {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(ErrorType.REGISTER_FAILED.toErrorResponse());
        }
    }


    // 多端登录实现
    @Override
    public ResponseEntity<?> loginUser(User user, LoginType type, HttpServletRequest request) {
        if (type == null) {
            logger.warn("登录失败：登录类型为空");
            return ResponseEntity.badRequest().body(ErrorType.LOGIN_TYPE_INVALID.toErrorResponse());
        }

        // 获取客户端类型
        String clientType = request.getHeader("Client-Type");
        clientType = (clientType == null || clientType.trim().isEmpty()) ? "h5" : clientType;
        logger.debug("处理登录请求，clientType: {}", clientType);

        User authenticatedUser = null;
        String loginIdentifier = null;
        String loginMethod = null;

        try {
            // 按登录类型认证
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
                    logger.warn("登录失败：未知登录类型，type: {}", type);
                    return ResponseEntity.status(400).body(ErrorType.LOGIN_FAILED.toErrorResponse());
            }

            // 生成令牌并存储
            Map<String, String> tokens = generateAndStoreTokens(authenticatedUser);
            String accessToken = tokens.get("accessToken");
            String refreshToken = tokens.get("refreshToken");

            // 更新登录时间
            authMapper.updateLastLoginTime(loginIdentifier, LocalDateTime.now(), loginMethod);
            logger.info("登录成功：{}，username: {}", loginMethod, authenticatedUser.getUsername());

            // 构建响应头（根据客户端类型设置Cookie）
            HttpHeaders headers = new HttpHeaders();
            if ("app".equals(clientType) || "h5".equals(clientType)) {
                headers = createTokenCookies(accessToken, refreshToken);
            }

            // 构建响应体
            Map<String, Object> response = new HashMap<>();
            response.put("code", "200");
            response.put("message", "登录成功");
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("expiresIn", accessTokenExpirationTime / 1000);
            response.put("userInfo", BeanUtil.beanToMap(authenticatedUser, false, true));

            return ResponseEntity.ok().headers(headers).body(response);

        } catch (AuthenticationException e) {
            logger.warn("登录认证失败：{}，identifier: {}", e.getErrorResponse(), loginIdentifier);
            return ResponseEntity.status(e.getHttpStatus()).body(e.getErrorResponse());
        } catch (IllegalArgumentException e) {
            logger.warn("登录参数异常：{}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorData(e.getMessage(),400));
        } catch (Exception e) {
            logger.error("登录系统异常", e);
            return ResponseEntity.status(500).body(ErrorType.LOGIN_FAILED.toErrorResponse());
        }
    }


    // 多端登出实现
    @Override
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String clientType = request.getHeader("Client-Type");
        clientType = (clientType == null) ? "h5" : clientType;
        logger.debug("处理登出请求，clientType: {}", clientType);

        // 提取refreshToken
        String refreshToken = extractRefreshTokenByClient(request, clientType);
        String username = "未知用户";

        try {
            // 清除Redis缓存
            if (refreshToken != null) {
                String userIdStr = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + refreshToken);
                if (userIdStr != null) {
                    User user = authMapper.getUserById(Integer.parseInt(userIdStr));
                    if (user != null) username = user.getUsername();
                }

                stringRedisTemplate.delete(REFRESH_TOKEN_KEY + refreshToken);
                stringRedisTemplate.delete(stringRedisTemplate.keys(AUTH_USER_KEY + "*"));
                logger.info("登出成功：清除缓存，username: {}", username);
            }

            // 清除Cookie（仅App/H5）
            HttpHeaders headers = new HttpHeaders();
            if ("app".equals(clientType) || "h5".equals(clientType)) {
                headers = clearTokenCookies();
            }

            Map<String, String> response = new HashMap<>();
            response.put("code", "200");
            response.put("message", "登出成功");
            return ResponseEntity.ok().headers(headers).body(response);

        } catch (Exception e) {
            logger.error("登出异常，username: {}", username, e);
            return ResponseEntity.status(500).body(new ErrorData("登出失败，请重试",500 ));
        }
    }


    // 刷新令牌
    @Override
    public String refreshToken(String refreshToken) {
        // 处理小程序格式
        if (refreshToken.startsWith("refreshToken=")) {
            String original = refreshToken;
            refreshToken = refreshToken.split("=")[1].trim();
            logger.debug("处理小程序refreshToken：{} -> {}", original, refreshToken);
        }

        // 验证令牌有效性
        if (!jwt.validateRefreshToken(refreshToken)) {
            logger.warn("刷新令牌无效");
            throw new IllegalArgumentException("无效的刷新令牌");
        }

        // 检查Redis
        String userIdStr = stringRedisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + refreshToken);
        if (userIdStr == null) {
            logger.warn("刷新令牌已过期");
            throw new IllegalArgumentException("刷新令牌已过期");
        }

        // 获取用户信息
        Integer userId = Integer.parseInt(userIdStr);
        User user = authMapper.getUserById(userId);
        if (user == null) {
            logger.warn("用户不存在，userId: {}", userId);
            throw new IllegalArgumentException("用户不存在");
        }

        // 生成新accessToken
        String newAccessToken = jwt.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().toString(),
                user.getAvatar()
        );

        // 更新Redis缓存
        String userRedisKey = AUTH_USER_KEY + newAccessToken;
        Map<String, Object> userHash = new HashMap<>();
        userHash.put("id", user.getId().toString());
        userHash.put("username", user.getUsername());
        userHash.put("role", user.getRole().toString());
        userHash.put("avatar", user.getAvatar());
        stringRedisTemplate.opsForHash().putAll(userRedisKey, userHash);
        stringRedisTemplate.expire(userRedisKey, accessTokenExpirationTime, TimeUnit.MILLISECONDS);

        // 续约refreshToken
        stringRedisTemplate.expire(REFRESH_TOKEN_KEY + refreshToken,
                refreshTokenExpirationTime, TimeUnit.MILLISECONDS);

        logger.info("刷新令牌成功，userId: {}", userId);
        return newAccessToken;
    }


    // 私有工具方法：用户名密码认证
    private User authenticateByUsernamePassword(User user) throws AuthenticationException {
        User dbUser = authMapper.LoginVerification(user.getUsername());
        if (dbUser == null) {
            throw new AuthenticationException(404, ErrorType.USERNAME_ERROR);
        }
        if (!Encryption.verifyPassword(user.getPassword(), dbUser.getPassword())) {
            throw new AuthenticationException(401, ErrorType.PASSWORD_ERROR);
        }
        dbUser.setAvatar(authMapper.getImageUrlsByUserId(dbUser.getId()));
        return dbUser;
    }

    // 私有工具方法：邮箱验证码认证
    private User authenticateByEmailCode(User user) throws AuthenticationException {
        if (!isEmailUsed(user.getEmail())) {
            throw new AuthenticationException(404, ErrorType.EMAIL_NOT_REGISTERED);
        }
        if (!validateCode(user.getEmail(), user.getCode())) {
            throw new AuthenticationException(401, ErrorType.CODE_INVALID_FAILED);
        }
        User dbUser = authMapper.LoginVerification(user.getEmail());
        dbUser.setAvatar(authMapper.getImageUrlsByUserId(dbUser.getId()));
        return dbUser;
    }

    // 私有工具方法：邮箱密码认证
    private User authenticateByEmailPassword(User user) throws AuthenticationException {
        if (!isEmailUsed(user.getEmail())) {
            throw new AuthenticationException(404, ErrorType.EMAIL_NOT_REGISTERED);
        }
        User dbUser = authMapper.LoginVerification(user.getEmail());
        if (dbUser == null) {
            throw new AuthenticationException(404, ErrorType.EMAIL_NOT_REGISTERED);
        }
        if (!Encryption.verifyPassword(user.getPassword(), dbUser.getPassword())) {
            throw new AuthenticationException(401, ErrorType.PASSWORD_ERROR);
        }
        dbUser.setAvatar(authMapper.getImageUrlsByUserId(dbUser.getId()));
        return dbUser;
    }

    // 私有工具方法：生成令牌并存储
    private Map<String, String> generateAndStoreTokens(User user) {
        String accessToken = jwt.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().toString(),
                user.getAvatar()
        );
        String refreshToken = jwt.generateRefreshToken(
                user.getId(),
                user.getUsername(),
                user.getRole().toString(),
                user.getAvatar()
        );

        // 存储用户信息到Redis
        String userRedisKey = AUTH_USER_KEY + accessToken;
        Map<String, Object> userHash = new HashMap<>();
        userHash.put("id", user.getId().toString());
        userHash.put("username", user.getUsername());
        userHash.put("role", user.getRole().toString());
        userHash.put("avatar", user.getAvatar());
        stringRedisTemplate.opsForHash().putAll(userRedisKey, userHash);
        stringRedisTemplate.expire(userRedisKey, accessTokenExpirationTime, TimeUnit.MILLISECONDS);

        // 存储refreshToken到Redis
        stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY + refreshToken,
                user.getId().toString(),
                refreshTokenExpirationTime,
                TimeUnit.MILLISECONDS
        );

        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    // 私有工具方法：创建Token Cookie
    private HttpHeaders createTokenCookies(String accessToken, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();

        // accessToken Cookie
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .maxAge(accessTokenExpirationTime / 1000)
                .path("/")
                // .secure(true) // 生产环境启用HTTPS时打开
                // .sameSite("Lax")
                .build();

        // refreshToken Cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .maxAge(refreshTokenExpirationTime / 1000)
                .path("/")
                // .secure(true)
                // .sameSite("Lax")
                .build();

        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return headers;
    }

    // 私有工具方法：清除Token Cookie
    private HttpHeaders clearTokenCookies() {
        HttpHeaders headers = new HttpHeaders();

        ResponseCookie clearAccessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .maxAge(0)
                .path("/")
                .build();

        ResponseCookie clearRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .maxAge(0)
                .path("/")
                .build();

        headers.add(HttpHeaders.SET_COOKIE, clearAccessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString());
        return headers;
    }

    // 私有工具方法：验证邮箱格式
    private void validateEmailFormat(String email) {
        if (email == null || !Validation.isValidEmail(email)) {
            throw new IllegalArgumentException("邮箱地址不正确");
        }
    }

    // 私有工具方法：生成验证码
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // 私有工具方法：按客户端类型提取refreshToken
    private String extractRefreshTokenByClient(HttpServletRequest request, String clientType) {
        if ("app".equals(clientType) || "h5".equals(clientType)) {
            // 从Cookie提取
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        } else if ("miniprogram".equals(clientType)) {
            // 从小程序Authorization头提取
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String tokenStr = authHeader.substring(7).trim();
                if (tokenStr.startsWith("refreshToken=")) {
                    return tokenStr.split("=")[1].trim();
                }
            }
        }
        return null;
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
}
