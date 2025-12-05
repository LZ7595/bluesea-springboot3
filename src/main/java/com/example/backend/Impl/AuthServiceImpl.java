// com.example.backend.Impl.AuthServiceImpl.java
package com.example.backend.Impl;

import cn.hutool.core.convert.Convert;
import com.alibaba.fastjson.JSON;
import com.example.backend.Dao.AuthMapper;
import com.example.backend.Dao.UserMapper;
import com.example.backend.Model.Enum.LoginType;
import com.example.backend.Model.Enum.ErrorType;
import com.example.backend.Model.Response.ErrorData;
import com.example.backend.Model.Entity.User;
import com.example.backend.Model.Vo.IdentityVerifyRequest;
import com.example.backend.Model.Vo.OperationExecuteRequest;
import com.example.backend.Model.Vo.SendCodeRequest;
import com.example.backend.Model.Vo.UserInfo;
import com.example.backend.Service.AuthService;
import com.example.backend.Utils.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.backend.Utils.RedisConstants.*;
import static com.example.backend.Utils.SensitiveInfo.maskEmail;
import static com.example.backend.Utils.SensitiveInfo.maskPhone;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AuthMapper authMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private Common common;
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
    @Autowired
    private VerificationCodeUtil verificationCodeUtil;
    @Autowired
    private Encryption encryption;

    // 操作类型常量
    public static final int OPERATION_REGISTER = 0;
    public static final int OPERATION_LOGIN = 1;
    public static final int OPERATION_CHANGE_BINDING = 2;
    public static final int OPERATION_BINDING = 3;
    public static final int OPERATION_CHANGE_PASSWORD = 4;

    // 验证方式常量
    public static final int METHOD_EMAIL = 0;
    public static final int METHOD_PHONE = 1;
    public static final int METHOD_OLD_PWD = 2;


    // Redis键常量
    private static final String VERIFY_TOKEN_KEY = "verify_token:";
    private static final long VERIFY_TOKEN_EXPIRATION = 10 * 60; // 10分钟

    /**
     * 聚合接口：发送验证码
     */
    @Override
    public ResponseEntity<?> sendCode(SendCodeRequest request, HttpServletRequest servletRequest) {
        return verificationCodeUtil.sendCode(
                request.getTarget(),
                request.getMethod(),
                request.getOperation(),
                request.getCheckType(),
                servletRequest
        );
    }

    /**
     * 第一步：身份验证
     */
    @Override
    public ResponseEntity<?> verifyIdentity(IdentityVerifyRequest request, HttpServletRequest servletRequest) {
        try {
            logger.info("开始身份验证，request: {}", request);

            Integer userId = getUserIdByIdentifier(request.getTarget(), request.getMethod());
            if (userId == null) {
                if (request.getMethod() == METHOD_EMAIL) {
                    logger.warn("身份验证失败，绑定邮箱填写错误: {}", maskSensitiveInfo(request.getTarget(), request.getMethod()));
                    return ResponseEntity.status(400).body(createErrorResponse("绑定邮箱填写错误"));
                }else {
                    logger.warn("身份验证失败，绑定手机号填写错误: {}", maskSensitiveInfo(request.getTarget(), request.getMethod()));
                    return ResponseEntity.status(400).body(createErrorResponse("绑定手机号填写错误"));
                }
            }
            // 1. 验证操作
            ResponseEntity<?> verifyResult = verificationCodeUtil.verifyOperation(
                    request.getTarget(),
                    request.getCode(),
                    request.getMethod(),
                    request.getOperation(),
                    request.getCheckType()
            );

            if (verifyResult.getStatusCode().value() != 200) {
                logger.warn("身份验证失败: {}", verifyResult.getBody());
                return verifyResult;
            }

            // 2. 生成验证令牌
            String verifyToken = generateVerifyToken(request);
            logger.info("生成验证令牌成功，token: {}", verifyToken);

            // 3. 存储验证状态到Redis（10分钟有效）
            stringRedisTemplate.opsForValue().set(
                    VERIFY_TOKEN_KEY + verifyToken,
                    JSON.toJSONString(request),
                    Duration.ofMinutes(10)
            );

            // 4. 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("verifyToken", verifyToken);
            result.put("expiresIn", VERIFY_TOKEN_EXPIRATION);
            result.put("message", "身份验证成功");

            logger.info("身份验证成功，target: {}, operation: {}",
                    maskSensitiveInfo(request.getTarget(), request.getMethod()),
                    request.getOperation());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("身份验证异常，request: {}", request, e);
            return ResponseEntity.status(500).body(createErrorResponse("身份验证失败"));
        }
    }

    /**
     * 第二步：执行操作
     */
    @Override
    public ResponseEntity<?> executeOperation(OperationExecuteRequest request, HttpServletRequest servletRequest) {
        String verifyToken = request.getVerifyToken();
        try {
            logger.info("开始执行操作，request: {}", request);
            IdentityVerifyRequest verifyRequest = null;
            if (!verifyToken.equals("绑定不用token")) {
                // 1. 验证令牌
                if (!validateVerifyToken(verifyToken)) {
                    logger.warn("验证令牌无效或已过期，token: {}", verifyToken);
                    return ResponseEntity.status(400).body(createErrorResponse("验证令牌已过期或无效"));
                }

                // 2. 从Redis获取验证信息
                verifyRequest = getVerifyRequestFromRedis(verifyToken);
                if (verifyRequest == null) {
                    logger.warn("验证会话已过期，token: {}", verifyToken);
                    return ResponseEntity.status(400).body(createErrorResponse("验证会话已过期"));
                }

            } else {
                verifyRequest = new IdentityVerifyRequest();
                verifyRequest.setOperation(OPERATION_BINDING);
            }
            // 3. 执行具体业务操作
            ResponseEntity<?> result;
            boolean operationSuccess = false;

            try {
                switch (verifyRequest.getOperation()) {
                    case OPERATION_CHANGE_BINDING:
                        result = executeChangeBinding(verifyRequest, request, servletRequest);
                        break;
                    case OPERATION_CHANGE_PASSWORD:
                        result = executeChangePassword(verifyRequest, request);
                        break;
                    case OPERATION_BINDING:
                        result = executeBinding(request, servletRequest);
                        break;
                    default:
                        result = ResponseEntity.badRequest().body(createErrorResponse("不支持的操作类型"));
                }

                // 检查操作是否成功（状态码为2xx表示成功）
                if (result.getStatusCode().is2xxSuccessful()) {
                    operationSuccess = true;
                }

            } finally {
                // 4. 只有在操作成功时才清理验证令牌
                if (operationSuccess) {
                    cleanupVerifyToken(verifyToken);
                    logger.info("操作成功，已清理验证令牌，token: {}", verifyToken);
                } else {
                    logger.info("操作失败，保留验证令牌以便重试，token: {}", verifyToken);
                }
            }

            logger.info("操作执行完成，operation: {}, success: {}, result: {}",
                    verifyRequest.getOperation(), operationSuccess, result.getStatusCode());

            return result;

        } catch (Exception e) {
            logger.error("执行操作异常，request: {}", request, e);

            // 异常情况下不清理令牌，允许用户重试
            logger.info("操作异常，保留验证令牌以便重试，token: {}", verifyToken);

            return ResponseEntity.status(500).body(createErrorResponse("操作失败"));
        }
    }

    // ========== 私有方法：第一步相关 ==========

    /**
     * 生成验证令牌
     */
    private String generateVerifyToken(IdentityVerifyRequest request) {
        // 使用JWT生成令牌，包含必要信息
        Map<String, Object> claims = new HashMap<>();
        claims.put("target", request.getTarget());
        claims.put("method", request.getMethod());
        claims.put("operation", request.getOperation());
        claims.put("timestamp", System.currentTimeMillis());

        return jwt.generateVerifyToken(claims, VERIFY_TOKEN_EXPIRATION * 1000);
    }

    /**
     * 验证令牌有效性
     */
    private boolean validateVerifyToken(String token) {
        try {
            return jwt.validateVerifyToken(token) &&
                    stringRedisTemplate.hasKey(VERIFY_TOKEN_KEY + token);
        } catch (Exception e) {
            logger.error("验证令牌异常", e);
            return false;
        }
    }

    /**
     * 从Redis获取验证请求
     */
    private IdentityVerifyRequest getVerifyRequestFromRedis(String token) {
        try {
            String verifyInfo = stringRedisTemplate.opsForValue().get(VERIFY_TOKEN_KEY + token);
            if (verifyInfo == null) {
                return null;
            }
            return JSON.parseObject(verifyInfo, IdentityVerifyRequest.class);
        } catch (Exception e) {
            logger.error("解析验证请求异常", e);
            return null;
        }
    }

    /**
     * 清理验证令牌
     */
    private void cleanupVerifyToken(String token) {
        try {
            stringRedisTemplate.delete(VERIFY_TOKEN_KEY + token);
        } catch (Exception e) {
            logger.error("清理验证令牌异常", e);
        }
    }

    /**
     * 执行换绑操作
     */
    private ResponseEntity<?> executeChangeBinding(IdentityVerifyRequest verifyRequest, OperationExecuteRequest executeRequest, HttpServletRequest servletRequest) {
        try {
            // 从验证请求中获取用户ID
            Integer userId = getUserIdByIdentifier(verifyRequest.getTarget(), verifyRequest.getMethod());
            if (userId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("用户不存在"));
            }

            boolean isvalidate = verificationCodeUtil.validateCode(executeRequest.getNewTarget(), executeRequest.getCode());

            if (!isvalidate) {
                return ResponseEntity.badRequest().body(createErrorResponse("验证码错误"));
            }

            String newTarget = executeRequest.getNewTarget();
            Integer newMethod = executeRequest.getNewMethod();

            if (newTarget == null || newMethod == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("换绑参数不完整"));
            }

            // 解除原绑定
            int clearResult;
            if (verifyRequest.getMethod() == METHOD_EMAIL) {
                clearResult = authMapper.clearEmail(userId);
            } else if (verifyRequest.getMethod() == METHOD_PHONE) {
                clearResult = authMapper.clearPhone(userId);
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("不支持的验证方式"));
            }

            if (clearResult == 0) {
                logger.warn("解除原绑定失败，用户可能不存在或已被删除，userId: {}", userId);
                return ResponseEntity.badRequest().body(createErrorResponse("用户不存在或操作失败"));
            }

            // 绑定新账号
            int updateResult;
            if (newMethod == METHOD_EMAIL) {
                updateResult = authMapper.updateEmail(userId, newTarget);
            } else if (newMethod == METHOD_PHONE) {
                updateResult = authMapper.updatePhone(userId, newTarget);
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("不支持的验证方式"));
            }

            if (updateResult == 0) {
                logger.error("绑定新账号失败，userId: {}, newTarget: {}", userId, newTarget);
                return ResponseEntity.status(500).body(createErrorResponse("换绑失败"));
            }

            logger.info("换绑成功，userId: {}, 原方式: {}, 新方式: {}, 新目标: {}",
                    userId, verifyRequest.getMethod(), newMethod, maskSensitiveInfo(newTarget, newMethod));

            // 2. 精确更新缓存（通过accessToken）
            boolean cacheUpdated = updateUserCacheByToken(userId, executeRequest, servletRequest);

            if (!cacheUpdated) {
                logger.warn("缓存更新失败，但数据库已更新，userId: {}", userId);
            }

            return ResponseEntity.ok(createSuccessResponse("换绑成功"));

        } catch (Exception e) {
            logger.error("换绑操作异常", e);
            return ResponseEntity.status(500).body(createErrorResponse("换绑失败"));
        }
    }

    /**
     * 执行修改密码操作
     */
    private ResponseEntity<?> executeChangePassword(IdentityVerifyRequest verifyRequest, OperationExecuteRequest executeRequest) {
        try {
            logger.info("执行修改密码操作，验证请求: {}", verifyRequest);
            Integer userId = getUserIdByIdentifier(verifyRequest.getTarget(), verifyRequest.getMethod());
            if (userId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("用户不存在"));
            }

            String newPassword = executeRequest.getNewPassword();
            if (newPassword == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("新密码不能为空"));
            }

            // 执行密码修改逻辑
            String encryptedPassword = encryption.encryptPassword(newPassword);
            int updateResult = authMapper.updatePassword(userId, encryptedPassword);

            if (updateResult == 0) {
                return ResponseEntity.status(500).body(createErrorResponse("密码修改失败"));
            }

            logger.info("密码修改成功，userId: {}", userId);
            return ResponseEntity.ok(createSuccessResponse("密码修改成功"));

        } catch (Exception e) {
            logger.error("修改密码异常", e);
            return ResponseEntity.status(500).body(createErrorResponse("密码修改失败"));
        }
    }

    private ResponseEntity<?> executeBinding(OperationExecuteRequest request, HttpServletRequest servletRequest) {
        try {
            String accessToken = common.extractAccessTokenByClient(servletRequest);
            System.out.println("accessToken: " + accessToken);
            Integer userId = jwt.getIdFromToken(accessToken);
            if (userId == null)
                return ResponseEntity.badRequest().body(createErrorResponse("用户不存在"));
            boolean isvalidate = verificationCodeUtil.validateCode(request.getNewTarget(), request.getCode());

            if (!isvalidate) {
                return ResponseEntity.badRequest().body(createErrorResponse("验证码错误"));
            }

            String newTarget = request.getNewTarget();
            Integer newMethod = request.getNewMethod();

            if (newTarget == null || newMethod == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("换绑参数不完整"));
            }

            // 绑定新账号
            int updateResult;
            if (newMethod == METHOD_EMAIL) {
                updateResult = authMapper.updateEmail(userId, newTarget);
            } else if (newMethod == METHOD_PHONE) {
                updateResult = authMapper.updatePhone(userId, newTarget);
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("不支持的验证方式"));
            }

            if (updateResult == 0) {
                logger.error("绑定新账号失败，userId: {}, newTarget: {}", userId, newTarget);
                return ResponseEntity.status(500).body(createErrorResponse("绑定失败"));
            }

            logger.info("绑定成功，userId: {}, 方式: {}, 目标: {}",
                    userId, newMethod, maskSensitiveInfo(newTarget, newMethod));

            // 2. 精确更新缓存（通过accessToken）
            boolean cacheUpdated = updateUserCacheByToken(userId, request, servletRequest);

            if (!cacheUpdated) {
                logger.warn("缓存更新失败，但数据库已更新，userId: {}", userId);
            }

            return ResponseEntity.ok(createSuccessResponse("换绑成功"));

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("绑定异常", e);
            return ResponseEntity.status(500).body(createErrorResponse("绑定失败"));
        }
    }

    // 验证码相关方法实现（使用Redis）
    @Override
    public ResponseEntity<?> confirmChange(Integer userId, String info, String code, String type, Integer num) {
        try {
            Integer isRegistered = 0;
            boolean codeValid = false;

            if (type.equals("changeEmail")) {
                isRegistered = authMapper.selectEmail(info);
            } else if (type.equals("changePhone")) {
                isRegistered = authMapper.selectPhone(info);
            }

            if (isRegistered >= 1 && num == 1) {
                return ResponseEntity.status(404).body(ErrorType.EMAIL_REGISTERED.toErrorResponse());
            } else if (num == 1) {
                codeValid = verificationCodeUtil.validateCode(info, code);
                if (!codeValid) {
                    return ResponseEntity.status(404).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
                } else {
                    if (type.equals("changeEmail")) {
                        authMapper.updateEmail(userId, info);
                    } else if (type.equals("changePhone"))
                        authMapper.updatePhone(userId, info);
                }
            } else if (num == 0) {
                codeValid = verificationCodeUtil.validateCode(info, code);
                if (codeValid) {
                    return ResponseEntity.ok().body("验证成功");
                } else {
                    return ResponseEntity.status(404).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
                }
            } else {
                return ResponseEntity.status(404).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
            }

            return ResponseEntity.ok("操作成功");

        } catch (Exception e) {
            logger.error("验证换绑异常", e);
            return ResponseEntity.status(500).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
        }
    }

    // 验证码校验（使用Redis）
    @Override
    public boolean validateCode(String target, String code) {
        return verificationCodeUtil.validateCode(target, code);
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

    //手机号占用校验
    public boolean isPhoneUsed(String phone) {
        int result = authMapper.selectPhone(phone);
        boolean isUsed = result != 0;
        logger.debug("手机号占用校验：phone: {}, 状态: {}", phone, isUsed);
        return isUsed;
    }

    @Override
    public ResponseEntity<?> registerUser(User user) {
        try {
            // 参数验证
            ResponseEntity<?> validationResult = validateRegisterParams(user);
            if (validationResult != null) {
                return validationResult;
            }

            // 验证码验证
            if (!validateCode(user.getEmail() != null ? user.getEmail() : user.getPhone(), user.getCode())) {
                logger.warn("注册失败：验证码无效，target: {}", user.getEmail() != null ? user.getEmail() : user.getPhone());
                return ResponseEntity.status(404).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
            }

            // 加密密码
            user.setPassword(Encryption.encryptPassword(user.getPassword()));

            // 设置默认状态和注册时间
            user.setStatus(true); // 激活状态
            user.setRegisterTime(LocalDateTime.now());

            // 插入数据库
            Boolean res = authMapper.insert(user);

            if (res) {
                logger.info("注册成功：username: {}, email: {}, phone: {}",
                        user.getUsername(), user.getEmail(), user.getPhone());

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

    // 注册参数验证
    private ResponseEntity<?> validateRegisterParams(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            logger.warn("注册失败：用户名为空");
            return ResponseEntity.badRequest().body(ErrorType.USERNAME_EMPTY.toErrorResponse());
        }

        if (user.getPassword() == null || user.getPassword().length() < 8) {
            logger.warn("注册失败：密码长度不足8位");
            return ResponseEntity.badRequest().body(ErrorType.PASSWORD_TOO_SHORT.toErrorResponse());
        }

        if (user.getCode() == null || user.getCode().trim().isEmpty()) {
            logger.warn("注册失败：验证码为空");
            return ResponseEntity.badRequest().body(ErrorType.CODE_EMPTY.toErrorResponse());
        }

        // 检查邮箱和手机号至少有一个
        if (user.getEmail() == null && user.getPhone() == null) {
            logger.warn("注册失败：邮箱和手机号不能同时为空");
            return ResponseEntity.badRequest().body(ErrorType.EMAIL_OR_PHONE_REQUIRED.toErrorResponse());
        }

        // 邮箱验证
        if (user.getEmail() != null) {
            if (!Validation.isValidEmail(user.getEmail())) {
                logger.warn("注册失败：邮箱格式无效，email: {}", user.getEmail());
                return ResponseEntity.status(500).body(ErrorType.EMAIL_VERIFICATION_FAILED.toErrorResponse());
            }

            if (isEmailUsed(user.getEmail())) {
                logger.warn("注册失败：邮箱已占用，email: {}", user.getEmail());
                return ResponseEntity.status(409).body(ErrorType.EMAIL_REGISTERED.toErrorResponse());
            }
        }

        // 手机号验证
        if (user.getPhone() != null) {
            if (!Validation.isValidPhone(user.getPhone())) {
                logger.warn("注册失败：手机号格式无效，phone: {}", user.getPhone());
                return ResponseEntity.badRequest().body(ErrorType.PHONE_VERIFICATION_FAILED.toErrorResponse());
            }

            if (isPhoneUsed(user.getPhone())) {
                logger.warn("注册失败：手机号已占用，phone: {}", user.getPhone());
                return ResponseEntity.status(409).body(ErrorType.PHONE_REGISTERED.toErrorResponse());
            }
        }

        // 用户名验证
        if (isUsernameUsed(user.getUsername())) {
            logger.warn("注册失败：用户名已占用，username: {}", user.getUsername());
            return ResponseEntity.status(409).body(ErrorType.USERNAME_REGISTERED.toErrorResponse());
        }

        return null;
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
                case EMAIL_VERIFICATION:
                    loginIdentifier = user.getEmail();
                    loginMethod = "邮箱验证";
                    validateEmailFormat(user.getEmail());
                    authenticatedUser = authenticateByEmailCode(user);
                    break;
                case PHONE_VERIFICATION:
                    loginIdentifier = user.getPhone();
                    loginMethod = "手机验证";
                    validatePhoneFormat(user.getPhone());
                    authenticatedUser = authenticateByPhoneCode(user);
                    break;
                case PASSWORD:
                    // 统一密码登录：自动识别账号类型
                    loginMethod = "密码登录";
                    authenticatedUser = authenticateByPassword(user);
                    loginIdentifier = user.getAccount(); // 使用统一的account字段
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
            response.put("userId", authenticatedUser.getId());

            return ResponseEntity.ok().headers(headers).body(response);

        } catch (AuthenticationException e) {
            logger.warn("登录认证失败：{}，identifier: {}", e.getErrorResponse(), loginIdentifier);
            return ResponseEntity.status(e.getHttpStatus()).body(e.getErrorResponse());
        } catch (IllegalArgumentException e) {
            logger.warn("登录参数异常：{}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorData(e.getMessage(), 400));
        } catch (Exception e) {
            logger.error("登录系统异常", e);
            return ResponseEntity.status(500).body(ErrorType.LOGIN_FAILED.toErrorResponse());
        }
    }

    /**
     * 统一密码登录认证：自动识别账号类型
     */
    private User authenticateByPassword(User user) throws AuthenticationException {
        String account = user.getAccount();
        if (account == null || account.trim().isEmpty()) {
            throw new AuthenticationException(400, ErrorType.USERNAME_ERROR);
        }

        // 自动识别账号类型
        if (account.contains("@")) {
            // 邮箱登录
            validateEmailFormat(account);
            return authenticateByEmailPassword(account, user.getPassword());
        } else if (account.matches("^1[3-9]\\d{9}$")) {
            // 手机号登录
            validatePhoneFormat(account);
            return authenticateByPhonePassword(account, user.getPassword());
        } else {
            // 用户名登录
            return authenticateByUsernamePassword(account, user.getPassword());
        }
    }

    /**
     * 邮箱密码认证
     */
    private User authenticateByEmailPassword(String email, String password) throws AuthenticationException {
        User dbUser = authMapper.LoginVerificationByEmail(email);
        if (dbUser == null) {
            throw new AuthenticationException(404, ErrorType.EMAIL_NOT_REGISTERED);
        }

        if (!Encryption.verifyPassword(password, dbUser.getPassword())) {
            throw new AuthenticationException(400, ErrorType.PASSWORD_ERROR);
        }

        dbUser.setAvatar(authMapper.getImageUrlsByUserId(dbUser.getId()));
        return dbUser;
    }

    /**
     * 手机密码认证
     */
    private User authenticateByPhonePassword(String phone, String password) throws AuthenticationException {
        User dbUser = authMapper.LoginVerificationByPhone(phone);
        if (dbUser == null) {
            throw new AuthenticationException(404, ErrorType.PHONE_NOT_REGISTERED);
        }

        if (!Encryption.verifyPassword(password, dbUser.getPassword())) {
            throw new AuthenticationException(404, ErrorType.PASSWORD_ERROR);
        }

        dbUser.setAvatar(authMapper.getImageUrlsByUserId(dbUser.getId()));
        return dbUser;
    }

    /**
     * 用户名密码认证
     */
    private User authenticateByUsernamePassword(String username, String password) throws AuthenticationException {
        User dbUser = authMapper.LoginVerificationByUsername(username);
        if (dbUser == null) {
            throw new AuthenticationException(404, ErrorType.USERNAME_ERROR);
        }

        if (!Encryption.verifyPassword(password, dbUser.getPassword())) {
            throw new AuthenticationException(404, ErrorType.PASSWORD_ERROR);
        }

        dbUser.setAvatar(authMapper.getImageUrlsByUserId(dbUser.getId()));
        return dbUser;
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
            return ResponseEntity.status(500).body(new ErrorData("登出失败，请重试", 500));
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
        UserInfo user = userMapper.getUserInfo(userId);
        if (user == null) {
            logger.warn("用户不存在，userId: {}", userId);
            throw new IllegalArgumentException("用户不存在");
        }

        // 生成新accessToken
        String newAccessToken = jwt.generateAccessToken(
                user.getId()
        );

        // 更新Redis缓存
        String userRedisKey = AUTH_USER_KEY + newAccessToken;
        // 1. 将userInfo对象转为BeanMap（自动映射所有属性）
        BeanMap beanMap = BeanMap.create(user);

        // 2. 定义需要存入Redis的字段（按需筛选，避免冗余）
        String[] includeFields = {"id", "username", "role", "avatar", "email", "phone", "gender", "birthday", "status", "register_time"};

        // 3. 转换为目标Map，并处理类型（转为String，适配Redis存储）
        Map<String, Object> userHash = new HashMap<>();
        for (String field : includeFields) {
            if (beanMap.containsKey(field)) {
                Object value = beanMap.get(field);
                // 统一转为String（避免Redis存储类型问题，如Long转String）
                userHash.put(field, value != null ? value.toString() : null);
            }
        }
        stringRedisTemplate.opsForHash().putAll(userRedisKey, userHash);
        stringRedisTemplate.expire(userRedisKey, accessTokenExpirationTime, TimeUnit.MILLISECONDS);

        // 续约refreshToken
        stringRedisTemplate.expire(REFRESH_TOKEN_KEY + refreshToken,
                refreshTokenExpirationTime, TimeUnit.MILLISECONDS);

        logger.info("刷新令牌成功，userId: {}", userId);
        return newAccessToken;
    }

    // 私有工具方法：邮箱验证码认证
    private User authenticateByEmailCode(User user) throws AuthenticationException {
        if (!isEmailUsed(user.getEmail())) {
            throw new AuthenticationException(404, ErrorType.EMAIL_NOT_REGISTERED);
        }
        // 使用Redis验证码验证
        if (!validateCode(user.getEmail(), user.getCode())) {
            throw new AuthenticationException(404, ErrorType.CODE_INVALID_FAILED);
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
            throw new AuthenticationException(404, ErrorType.PASSWORD_ERROR);
        }
        dbUser.setAvatar(authMapper.getImageUrlsByUserId(dbUser.getId()));
        return dbUser;
    }

    // 私有工具方法：手机验证码认证
    private User authenticateByPhoneCode(User user) throws AuthenticationException {
        if (!isPhoneUsed(user.getPhone())) {
            throw new AuthenticationException(404, ErrorType.PHONE_NOT_REGISTERED);
        }
        // 使用Redis验证码验证
        if (!validateCode(user.getPhone(), user.getCode())) {
            throw new AuthenticationException(404, ErrorType.CODE_INVALID_FAILED);
        }
        User dbUser = authMapper.LoginVerification(user.getPhone());
        dbUser.setAvatar(authMapper.getImageUrlsByUserId(dbUser.getId()));
        return dbUser;
    }

    // 私有工具方法：生成令牌并存储
    private Map<String, String> generateAndStoreTokens(User user) {
        UserInfo userInfo = userMapper.getUserInfo(user.getId());
        String accessToken = jwt.generateAccessToken(
                userInfo.getId()
        );
        String refreshToken = jwt.generateRefreshToken(
                userInfo.getId()
        );

        // 存储用户信息到Redis
        String userRedisKey = AUTH_USER_KEY + accessToken;
        // 1. 将userInfo对象转为BeanMap（自动映射所有属性）
        BeanMap beanMap = BeanMap.create(userInfo);

        // 2. 定义需要存入Redis的字段（按需筛选，避免冗余）
        String[] includeFields = {"id", "username", "role", "avatar", "email", "phone", "gender", "birthday", "status", "register_time"};

        // 3. 转换为目标Map，并处理类型（转为String，适配Redis存储）
        Map<String, Object> userHash = new HashMap<>();
        for (String field : includeFields) {
            if (beanMap.containsKey(field)) {
                Object value = beanMap.get(field);
                // 统一转为String（避免Redis存储类型问题，如Long转String）
                userHash.put(field, value != null ? value.toString() : null);
            }
        }
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

    // 私有工具方法：验证手机号格式
    private void validatePhoneFormat(String phone) {
        if (phone == null || !Validation.isValidPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
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
            // 从小程序accessToken头提取
            String authHeader = request.getHeader("accessToken");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String tokenStr = authHeader.substring(7).trim();
                if (tokenStr.startsWith("refreshToken=")) {
                    return tokenStr.split("=")[1].trim();
                }
            }
        }
        return null;
    }

    // 辅助方法
    private String maskSensitiveInfo(String target, int method) {
        if (method == METHOD_EMAIL) {
            int atIndex = target.indexOf('@');
            if (atIndex > 3) {
                return target.substring(0, 3) + "***" + target.substring(atIndex);
            }
            return target.substring(0, Math.min(atIndex, target.length())) + "***";
        } else if (method == METHOD_PHONE) {
            if (target.length() == 11) {
                return target.substring(0, 3) + "****" + target.substring(7);
            }
            return "***" + target.substring(target.length() - 4);
        }
        return target;
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
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

    /**
     * 根据标识符获取用户ID
     */
    private Integer getUserIdByIdentifier(String identifier, int method) {
        if (method == METHOD_EMAIL) {
            User user = authMapper.LoginVerificationByEmail(identifier);
            return user != null ? user.getId() : null;
        } else if (method == METHOD_PHONE) {
            User user = authMapper.LoginVerificationByPhone(identifier);
            return user != null ? user.getId() : null;
        } else if (method == METHOD_OLD_PWD) {
            return Integer.valueOf(identifier);
        }
        return null;
    }

    private boolean updateUserCacheByToken(Integer userId, OperationExecuteRequest executeRequest, HttpServletRequest request) {
        try {
            // 1. 使用相同的方式提取accessToken
            String accessToken = common.extractAccessTokenByClient(request);
            if (accessToken == null) {
                logger.warn("无法获取accessToken，跳过缓存更新");
                return false;
            }

            String redisKey = AUTH_USER_KEY + accessToken;

            // 2. 检查缓存是否存在
            if (!stringRedisTemplate.hasKey(redisKey)) {
                logger.warn("缓存键不存在: {}", redisKey);
                return false;
            }

            // 3. 验证缓存中的用户ID是否匹配
            Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(redisKey);
            if (userMap == null || userMap.isEmpty()) {
                logger.warn("缓存数据为空: {}", redisKey);
                return false;
            }

            Integer cachedUserId = Convert.toInt(userMap.get("id"));
            if (!userId.equals(cachedUserId)) {
                logger.warn("用户ID不匹配，缓存userId: {}，请求userId: {}", cachedUserId, userId);
                return false;
            }

            // 4. 更新缓存（与getUserInfo相同的字段结构）
            Map<String, Object> updates = new HashMap<>();
            if (executeRequest.getNewMethod() == METHOD_EMAIL) {
                updates.put("email", maskEmail(executeRequest.getNewTarget()));
            } else if (executeRequest.getNewMethod() == METHOD_PHONE) {
                updates.put("phone", maskPhone(executeRequest.getNewTarget()));
            }

            stringRedisTemplate.opsForHash().putAll(redisKey, updates);

            // 5. 刷新过期时间（可选）
            stringRedisTemplate.expire(redisKey, accessTokenExpirationTime, TimeUnit.MILLISECONDS);

            logger.info("用户缓存更新成功，userId: {}, redisKey: {}", userId, redisKey);
            return true;

        } catch (Exception e) {
            logger.error("通过token更新用户缓存失败，userId: {}", userId, e);
            return false;
        }
    }
}