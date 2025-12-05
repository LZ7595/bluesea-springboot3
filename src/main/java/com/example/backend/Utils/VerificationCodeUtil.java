// com.example.backend.Utils.VerificationCodeUtil.java
package com.example.backend.Utils;

import com.example.backend.Dao.AuthMapper;
import com.example.backend.Dao.UserMapper;
import com.example.backend.Model.Vo.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class VerificationCodeUtil {

    @Autowired
    private AuthMapper authMapper;

    private final StringRedisTemplate stringRedisTemplate;
    private final Email emailService;
    private final SmsSender smsSender;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private Common common;

    @Autowired
    private Jwt jwt;

    // Redis键前缀
    private static final String AUTH_CODE_KEY = "auth:code:";
    private static final String FREQUENCY_KEY = "auth:frequency:";
    private static final long CODE_EXPIRE_TIME = 300; // 5分钟
    private static final long FREQUENCY_LIMIT = 60; // 60秒内不能重复发送

    // 操作类型
    public static final int OPERATION_REGISTER = 0;      // 注册
    public static final int OPERATION_LOGIN = 1;         // 登录
    public static final int OPERATION_CHANGE_BINDING = 2; // 换绑
    public static final int OPERATION_BINDING = 3; // 绑定
    public static final int OPERATION_CHANGE_PASSWORD = 4; // 修改密码

    // 验证方式
    public static final int METHOD_EMAIL = 0;    // 邮箱验证
    public static final int METHOD_PHONE = 1;    // 手机验证
    public static final int METHOD_OLD_PWD = 2;  // 旧密码验证

    @Autowired
    public VerificationCodeUtil(StringRedisTemplate stringRedisTemplate,
                                Email emailService,
                                SmsSender smsSender) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.emailService = emailService;
        this.smsSender = smsSender;
    }

    /**
     * 聚合接口：发送验证码
     */
    public ResponseEntity<?> sendCode(String target, int method, int operation, Integer checkType, HttpServletRequest servletRequest) {
        try {
            // 参数验证
            ResponseEntity<?> validationResult = validateParameters(target, method, operation, checkType, servletRequest);
            log.info("开始验证参数 - target: {}, method: {}, operation: {}, checkType: {}",
                    target, method, operation, checkType);
            log.info(String.valueOf(validationResult));
            if (validationResult != null) {
                return validationResult;
            }

            // 检查发送频率
            if (isFrequencyLimited(target)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(createErrorResponse("发送过于频繁，请60秒后再试"));
            }

            // 生成验证码
            String code = generateCode();
            System.out.println("生成验证码：" + code + "，操作：" + getOperationName(operation) +
                    "，方式：" + getMethodName(method) + "，目标：" + target);

            // 发送验证码
            boolean sendResult = sendCodeByMethod(target, code, method, operation);
            if (!sendResult) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("验证码发送失败"));
            }

            // 存储到Redis
            storeCodeInRedis(target, code, method, operation, checkType);

            // 设置频率限制
            setFrequencyLimit(target);

            System.out.println("验证码发送成功");

            return ResponseEntity.ok(createSuccessResponse("验证码发送成功", target, method, operation, checkType));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("发送验证码异常，target: " + target + ", error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("系统错误，请稍后重试"));
        }
    }

    /**
     * 验证验证码
     */
    public boolean validateCode(String target, String code) {
        try {
            // 构建Redis key（尝试多种可能的key格式）
            String[] possibleKeys = {
                    AUTH_CODE_KEY + target, // 简单格式
                    AUTH_CODE_KEY + target + ":*" // 带操作类型的格式
            };

            for (String keyPattern : possibleKeys) {
                // 获取匹配的key
                String actualKey = findRedisKey(keyPattern);
                if (actualKey != null) {
                    String storedCode = stringRedisTemplate.opsForValue().get(actualKey);
                    if (storedCode != null && storedCode.equals(code)) {
                        // 验证成功，删除验证码（一次性使用）
                        stringRedisTemplate.delete(actualKey);
                        System.out.println("验证码验证成功，target: " + target);
                        return true;
                    }
                }
            }

            System.out.println("验证码验证失败，target: " + target + ", code: " + code);
            return false;

        } catch (Exception e) {
            System.err.println("验证验证码异常，target: " + target + ", error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 聚合接口：验证操作
     */
    public ResponseEntity<?> verifyOperation(String target, String code, int method,
                                             int operation, Integer checkType) {
        try {
            // 旧密码验证特殊处理
            if (method == METHOD_OLD_PWD) {
                return verifyOldPassword(target, code, operation);
            }

            // 验证码验证
            if (validateCode(target, code)) {
                return ResponseEntity.ok(createSuccessResponse("验证成功", target, method, operation, checkType));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("验证码错误或已过期"));
            }

        } catch (Exception e) {
            System.err.println("验证操作异常，target: " + target + ", error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("验证失败"));
        }
    }

    // 辅助方法：查找Redis key
    private String findRedisKey(String pattern) {
        try {
            // 使用keys命令查找匹配的key（注意：生产环境慎用keys，数据量大时用scan）
            var keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                return keys.iterator().next();
            }
        } catch (Exception e) {
            System.err.println("查找Redis key异常，pattern: " + pattern + ", error: " + e.getMessage());
        }
        return null;
    }

    // 其他辅助方法保持不变...
    private ResponseEntity<?> validateParameters(String target, int method, int operation, Integer checkType, HttpServletRequest servletRequest) {
        if (target == null || target.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("目标不能为空"));
        }

        if (method < 0 || method > 2) {
            return ResponseEntity.badRequest().body(createErrorResponse("验证方式参数错误"));
        }

        if (operation < 0 || operation > 4) {
            return ResponseEntity.badRequest().body(createErrorResponse("操作类型参数错误"));
        }

        // 换绑操作需要checkType
        if (operation == OPERATION_CHANGE_BINDING && checkType == null) {
            return ResponseEntity.badRequest().body(createErrorResponse("换绑操作需要指定检查类型"));
        }

        // 邮箱/手机格式验证
        if (method == METHOD_EMAIL && !Validation.isValidEmail(target)) {
            return ResponseEntity.badRequest().body(createErrorResponse("邮箱格式无效"));
        }

        if (method == METHOD_PHONE && !Validation.isValidPhone(target)) {
            return ResponseEntity.badRequest().body(createErrorResponse("手机号格式无效"));
        }

        if ((operation == OPERATION_CHANGE_PASSWORD && checkType == 0) || (operation == OPERATION_CHANGE_BINDING && checkType == 0)) {
            String accessToken = common.extractAccessTokenByClient(servletRequest);
            Integer userId = jwt.getIdFromToken(accessToken);
            if (userId == null) {
                return null;
            }

            // 从数据库查询用户信息
            UserInfo userInfo = userMapper.getUserInfo(userId);

            if (method == METHOD_EMAIL) {
                if (!userInfo.getEmail().equals(target)) {
                    return ResponseEntity.badRequest().body(createErrorResponse("邮箱不匹配"));
                }
            } else if (method == METHOD_PHONE) {
                boolean phoneMatch = userInfo.getPhone() != null && userInfo.getPhone().equals(target);
                log.info(String.valueOf(phoneMatch));
                if (!userInfo.getPhone().equals(target)) {
                    return ResponseEntity.badRequest().body(createErrorResponse("手机号不匹配"));
                }
            }
        }

        if ((operation == OPERATION_CHANGE_BINDING && checkType == 1) || operation == OPERATION_BINDING) {
            if (method == METHOD_EMAIL) {
                int emailCount = authMapper.getEmailCount(target);
                if (emailCount > 0) {
                    return ResponseEntity.badRequest().body(createErrorResponse("邮箱已被绑定"));
                }
            } else if (method == METHOD_PHONE) {
                int phoneCount = authMapper.getPhoneCount(target);
                if (phoneCount > 0) {
                    return ResponseEntity.badRequest().body(createErrorResponse("手机号已被绑定"));
                }
            }
        }
        return null;
    }

    private boolean sendCodeByMethod(String target, String code, int method, int operation) {
        String operationName = getOperationName(operation);

        if (method == METHOD_EMAIL) {
            try {
                emailService.sendEmail(target, code, operationName);
                return true;
            } catch (Exception e) {
                System.err.println("邮件发送失败: " + e.getMessage());
                return false;
            }
        } else if (method == METHOD_PHONE) {
//            return smsSender.sendSms(target, code);
            return true; // 模拟短信发送成功
        }

        return true; // 旧密码验证不需要发送验证码
    }

    private void storeCodeInRedis(String target, String code, int method, int operation, Integer checkType) {
        String redisKey = buildRedisKey(target, method, operation, checkType);
        stringRedisTemplate.opsForValue().set(redisKey, code, CODE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    private String buildRedisKey(String target, int method, int operation, Integer checkType) {
        String key = AUTH_CODE_KEY + target;
        // 添加操作类型信息，便于区分不同用途的验证码
        if (operation >= 0) {
            key += ":" + operation;
        }
        if (checkType != null) {
            key += ":" + checkType;
        }
        return key;
    }

    private ResponseEntity<?> verifyOldPassword(String target, String oldPassword, int operation) {
        String oldPasswordBySql = authMapper.getPasswordByUserId(Integer.valueOf(target));

        boolean isValid = Encryption.verifyPassword(oldPassword, oldPasswordBySql); // 实际应从数据库验证

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("旧密码错误"));
        }

        return ResponseEntity.ok(createSuccessResponse("旧密码验证成功", target, METHOD_OLD_PWD, operation, null));
    }

    private boolean isFrequencyLimited(String target) {
        String frequencyKey = FREQUENCY_KEY + target;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(frequencyKey));
    }

    private void setFrequencyLimit(String target) {
        String frequencyKey = FREQUENCY_KEY + target;
        stringRedisTemplate.opsForValue().set(frequencyKey, "1", FREQUENCY_LIMIT, TimeUnit.SECONDS);
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }

    private String getOperationName(int operation) {
        switch (operation) {
            case OPERATION_REGISTER:
                return "注册";
            case OPERATION_LOGIN:
                return "登录";
            case OPERATION_CHANGE_BINDING:
                return "换绑";
            case OPERATION_BINDING:
                return "绑定";
            case OPERATION_CHANGE_PASSWORD:
                return "修改密码";
            default:
                return "操作";
        }
    }

    private String getMethodName(int method) {
        switch (method) {
            case METHOD_EMAIL:
                return "邮箱";
            case METHOD_PHONE:
                return "手机";
            case METHOD_OLD_PWD:
                return "旧密码";
            default:
                return "未知";
        }
    }

    private Map<String, Object> createSuccessResponse(String message, String target,
                                                      int method, int operation, Integer checkType) {
        if (checkType == null) {
            checkType = 0; // 或其他默认值
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", Map.of(
                "target", maskSensitiveInfo(target, method),
                "method", method,
                "methodName", getMethodName(method),
                "operation", operation,
                "operationName", getOperationName(operation),
                "checkType", checkType,
                "expireTime", CODE_EXPIRE_TIME
        ));
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

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
        return target; // 旧密码验证不隐藏
    }
}