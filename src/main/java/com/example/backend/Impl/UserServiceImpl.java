package com.example.backend.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.example.backend.Entity.Enum.ErrorType;
import com.example.backend.Entity.User;
import com.example.backend.Entity.UserInfo;
import com.example.backend.Entity.UserSecurity;
import com.example.backend.Utils.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.backend.Dao.UserMapper;
import com.example.backend.Service.UserService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.example.backend.Utils.RedisConstants.AUTH_USER_KEY;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Resource
    private Jwt jwt;

    @Resource
    private Cookie cookie;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpirationTime;

    @Autowired
    private Email emailsend;
    private final Map<String, String> verificationCodes = new HashMap<>();
    private final Map<String, LocalDateTime> codeExpiration = new HashMap<>();

    private static final long CODE_EXPIRATION_TIME = 5 * 60 * 1000; // 验证码过期时间，5分钟

    // 根据用户 ID 获取用户综合信息
    public Optional<UserInfo> getUserInfoById(Integer userId) {
        return userMapper.getUserInfoById(userId);
    }

    public ResponseEntity<?> updateUserInfo(Integer userId, String username, String gender, Date birthday, String avatar) {
        try {
            userMapper.updateUsername(userId, username);
            userMapper.updateBirthday(userId, new java.sql.Date(birthday.getTime()));
            userMapper.updateGender(userId, gender);
            userMapper.updateAvatar(userId, avatar);
            return ResponseEntity.ok("修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("修改失败");
        }
    }

    public ResponseEntity<?> getSecurityInfo(Integer userId) {
        try {
            UserSecurity userSecurityInfo = userMapper.getSecurityInfo(userId);
            return ResponseEntity.ok().body(userSecurityInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败");
        }
    }

    public ResponseEntity<?> sendVerificationCode(String info, String type, Integer num) {
        try {
            if (type.equals("changeEmail")) {
                if (num == 0) {
                    String code = generateCode();
                    verificationCodes.put(info, code);
                    LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(CODE_EXPIRATION_TIME / 1000);
                    codeExpiration.put(info, expirationTime);
                    System.out.println(info + code);
                    emailsend.sendEmail(info, code, "换绑验证");
                    ResponseEntity<?> storeResult = storeCodeInDatabase(info, code, expirationTime, type);
                    if (storeResult.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                        return storeResult;
                    }
                } else if (num == 1) {
                    Integer isEmailRegistered = userMapper.selectEmail(info);
                    System.out.println(isEmailRegistered);
                    if (isEmailRegistered == 1) {
                        return ResponseEntity.status(404)
                                .body(ErrorType.EMAIL_REGISTERED.toErrorResponse());
                    } else {
                        String code = generateCode();
                        verificationCodes.put(info, code);
                        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(CODE_EXPIRATION_TIME / 1000);
                        codeExpiration.put(info, expirationTime);
                        System.out.println(info + code);
                        emailsend.sendEmail(info, code, "换绑验证");
                        ResponseEntity<?> storeResult = storeCodeInDatabase(info, code, expirationTime, type);
                        if (storeResult.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                            return storeResult;
                        }
                    }
                }
            } else if (type.equals("changePhone")) {
                if (num == 0) {
                    String code = generateCode();
                    verificationCodes.put(info, code);
                    LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(CODE_EXPIRATION_TIME / 1000);
                    codeExpiration.put(info, expirationTime);
                    boolean isSent = SmsSender.sendSms(info, code);
                    if (!isSent) {
                        return ResponseEntity.status(500).body(ErrorType.CODE_SENDING_FAILED.toErrorResponse());
                    }
                    ResponseEntity<?> storeResult = storeCodeInDatabase(info, code, expirationTime, type);
                    if (storeResult.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                        return storeResult;
                    }
                } else if (num == 1) {
                    Integer isPhoneRegistered = userMapper.selectPhone(info);
                    if (isPhoneRegistered == 1) {
                        return ResponseEntity.status(404)
                                .body(ErrorType.EMAIL_REGISTERED.toErrorResponse());
                    } else {
                        String code = generateCode();
                        verificationCodes.put(info, code);
                        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(CODE_EXPIRATION_TIME / 1000);
                        codeExpiration.put(info, expirationTime);
                        boolean isSent = SmsSender.sendSms(info, code);
                        if (!isSent) {
                            return ResponseEntity.status(500).body(ErrorType.CODE_SENDING_FAILED.toErrorResponse());
                        }
                        ResponseEntity<?> storeResult = storeCodeInDatabase(info, code, expirationTime, type);
                        if (storeResult.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                            return storeResult;
                        }
                    }
                }
            } else if (type.equals("changePasswordByEmail")) {
                Integer isEmailRegistered = userMapper.selectEmail(info);
                if (isEmailRegistered != 1) {
                    return ResponseEntity.status(404)
                            .body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
                }
                String code = generateCode();
                verificationCodes.put(info, code);
                LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(CODE_EXPIRATION_TIME / 1000);
                codeExpiration.put(info, expirationTime);
                System.out.println(info + code);
                emailsend.sendEmail(info, code, "修改密码验证");
                ResponseEntity<?> storeResult = storeCodeInDatabase(info, code, expirationTime, type);
                if (storeResult.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                    return storeResult;
                }
            } else if (type.equals("changePasswordByPhone")) {
                Integer isPhoneRegistered = userMapper.selectPhone(info);
                if (isPhoneRegistered != 1) {
                    return ResponseEntity.status(404)
                            .body(ErrorType.PHONE_NOT_REGISTERED.toErrorResponse());
                }
                String code = generateCode();
                verificationCodes.put(info, code);
                LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(CODE_EXPIRATION_TIME / 1000);
                codeExpiration.put(info, expirationTime);
                boolean isSent = SmsSender.sendSms(info, code);
                if (!isSent) {
                    return ResponseEntity.status(500).body(ErrorType.CODE_SENDING_FAILED.toErrorResponse());
                }
                ResponseEntity<?> storeResult = storeCodeInDatabase(info, code, expirationTime, type);
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

    private ResponseEntity<?> storeCodeInDatabase(String info, String code, LocalDateTime expirationTime, String type) {
        // 先尝试更新已存在的记录
        System.out.println(code);
        System.out.println(expirationTime);
        try {
            if (type.equals("changeEmail")) {
                Integer rowsUpdated = userMapper.updateEmailCode(code, expirationTime, info);
                System.out.println(rowsUpdated);
                if (rowsUpdated == 0) {
                    return ResponseEntity.status(404)
                            .body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
                }
            } else if (type.equals("changePhone")) {
                Integer rowsUpdated = userMapper.updatePhoneCode(code, expirationTime, info);
                System.out.println(rowsUpdated);
                if (rowsUpdated == 0) {
                    return ResponseEntity.status(404)
                            .body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
                }
            } else if (type.equals("changePasswordByEmail")) {
                Integer rowsUpdated = userMapper.updateEmailCode(code, expirationTime, info);
                if (rowsUpdated == 0) {
                    return ResponseEntity.status(404)
                            .body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
                }
            } else if (type.equals("changePasswordByPhone")) {
                Integer rowsUpdated = userMapper.updatePhoneCode(code, expirationTime, info);
                if (rowsUpdated == 0) {
                    return ResponseEntity.status(404)
                            .body(ErrorType.PHONE_NOT_REGISTERED.toErrorResponse());
                }
            }

        } catch (Exception e) {
            // 更详细的异常日志记录
            System.err.println("存储验证码时发生异常: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(ErrorType.CODE_INSERT_FAILED.toErrorResponse());
        }
        return ResponseEntity.ok("验证码已存储");
    }

    public ResponseEntity<?> confirmChange(Integer userId, String info, String code, String type, Integer num) {
        try {
            String storedCode = null;
            Integer isRegistered = 0;
            boolean codeValid = false;

            if (type.equals("changeEmail")) {
                storedCode = num == 0 ? userMapper.selectEmailCode(info) : null;
                isRegistered = userMapper.selectEmail(info);
            } else if (type.equals("changePhone")) {
                storedCode = num == 0 ? userMapper.selectPhoneCode(info) : null;
                isRegistered = userMapper.selectPhone(info);
            }

            if (isRegistered >= 1 && num == 1) {
                return ResponseEntity.status(404)
                        .body(ErrorType.EMAIL_REGISTERED.toErrorResponse());
            } else if (num == 1) {
                codeValid = validateCode(info, code);
                if (!codeValid) {
                    return ResponseEntity.status(404)
                            .body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
                } else {
                    if (type.equals("changeEmail")) {
                        userMapper.updateEmail(userId, info);
                    } else if (type.equals("changePhone"))
                        userMapper.updatePhone(userId, info);
                }
            } else if (storedCode != null && storedCode.equals(code)) {
                clearCodeFromDatabase(userId);
                return ResponseEntity.ok().body("验证成功");
            } else {
                return ResponseEntity.status(404)
                        .body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
        }
        return null;
    }

    @Override
    public boolean validateCode(String info, String code) {
        LocalDateTime expiration = codeExpiration.get(info);
        if (expiration == null || expiration.isBefore(LocalDateTime.now())) {
            return false;
        }
        return verificationCodes.get(info).equals(code);
    }

    private void clearCodeFromDatabase(Integer id) {
        userMapper.clearCode(id);
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
        userMapper.deleteExpiredCodes(now);
    }

    @Override
    public ResponseEntity<?> confirmChangePasswordByOldPassword(Integer userId, String oldPassword) {
        try {
            String storedPassword = userMapper.getPasswordById(userId);
            if (storedPassword != null && Encryption.verifyPassword(oldPassword, storedPassword)) {
                return ResponseEntity.ok("旧密码验证通过");
            }
            return ResponseEntity.status(404).body(ErrorType.OLD_PASSWORD_INCORRECT.toErrorResponse());
        } catch (Exception e) {
            System.err.println("验证旧密码时发生异常: " + e.getMessage());
            return ResponseEntity.status(500).body(ErrorType.PASSWORD_VERIFICATION_FAILED.toErrorResponse());
        }
    }

    @Override
    public ResponseEntity<?> confirmChangePasswordByEmail(Integer userId, String email, String code) {
        try {
            Integer isEmailRegistered = userMapper.selectEmail(email);
            if (isEmailRegistered != 1) {
                return ResponseEntity.status(404).body(ErrorType.EMAIL_NOT_REGISTERED.toErrorResponse());
            }
            String storedCode = userMapper.selectEmailCode(email);
            if (storedCode != null && storedCode.equals(code) && validateCode(email, code)) {
                return ResponseEntity.ok("邮箱验证通过");
            }
            return ResponseEntity.status(404).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
        } catch (Exception e) {
            System.err.println("验证邮箱验证码时发生异常: " + e.getMessage());
            return ResponseEntity.status(500).body(ErrorType.CODE_VERIFICATION_FAILED.toErrorResponse());
        }
    }

    @Override
    public ResponseEntity<?> confirmChangePasswordByPhone(Integer userId, String phone, String code) {
        try {
            Integer isPhoneRegistered = userMapper.selectPhone(phone);
            if (isPhoneRegistered != 1) {
                return ResponseEntity.status(404).body(ErrorType.PHONE_NOT_REGISTERED.toErrorResponse());
            }
            String storedCode = userMapper.selectPhoneCode(phone);
            if (storedCode != null && storedCode.equals(code) && validateCode(phone, code)) {
                return ResponseEntity.ok("手机验证通过");
            }
            return ResponseEntity.status(404).body(ErrorType.CODE_INVALID_FAILED.toErrorResponse());
        } catch (Exception e) {
            System.err.println("验证手机验证码时发生异常: " + e.getMessage());
            return ResponseEntity.status(500).body(ErrorType.CODE_VERIFICATION_FAILED.toErrorResponse());
        }
    }

    @Override
    public ResponseEntity<?> changePasswordByOldPassword(Integer userId, String oldPassword, String newPassword) {
        ResponseEntity<?> confirmResult = confirmChangePasswordByOldPassword(userId, oldPassword);
        if (confirmResult.getStatusCode() == HttpStatus.OK) {
            try {
                String encryptedNewPassword = Encryption.encryptPassword(newPassword);
                userMapper.updatePassword(userId, encryptedNewPassword);
                return ResponseEntity.ok("密码修改成功");
            } catch (Exception e) {
                System.err.println("修改密码时发生异常: " + e.getMessage());
                return ResponseEntity.status(500).body(ErrorType.PASSWORD_UPDATE_FAILED.toErrorResponse());
            }
        }
        return confirmResult;
    }

    @Override
    public ResponseEntity<?> changePasswordByEmail(Integer userId, String email, String code, String newPassword) {
        ResponseEntity<?> confirmResult = confirmChangePasswordByEmail(userId, email, code);
        if (confirmResult.getStatusCode() == HttpStatus.OK) {
            try {
                String encryptedNewPassword = Encryption.encryptPassword(newPassword);
                userMapper.updatePassword(userId, encryptedNewPassword);
                return ResponseEntity.ok("密码修改成功");
            } catch (Exception e) {
                System.err.println("修改密码时发生异常: " + e.getMessage());
                return ResponseEntity.status(500).body(ErrorType.PASSWORD_UPDATE_FAILED.toErrorResponse());
            }
        }
        return confirmResult;
    }

    @Override
    public ResponseEntity<?> changePasswordByPhone(Integer userId, String phone, String code, String newPassword) {
        ResponseEntity<?> confirmResult = confirmChangePasswordByPhone(userId, phone, code);
        if (confirmResult.getStatusCode() == HttpStatus.OK) {
            try {
                String encryptedNewPassword = Encryption.encryptPassword(newPassword);
                userMapper.updatePassword(userId, encryptedNewPassword);
                return ResponseEntity.ok("密码修改成功");
            } catch (Exception e) {
                System.err.println("修改密码时发生异常: " + e.getMessage());
                return ResponseEntity.status(500).body(ErrorType.PASSWORD_UPDATE_FAILED.toErrorResponse());
            }
        }
        return confirmResult;
    }

    public ResponseEntity<?> searchUserByUserId(Integer userId) {
        try {
            User user = userMapper.searchUserByUserId(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(null);
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            System.err.println("查询用户信息时发生异常: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }

        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    public UserInfo getUserInfo(HttpServletRequest request) {
        String accessToken = cookie.getCookieValue(request, "accessToken");

        String redisKey = AUTH_USER_KEY + request;

        // 1. 尝试从 Redis 获取用户信息
        Map<Object, Object> getUserMap = stringRedisTemplate.opsForHash().entries(redisKey);
        UserInfo userInfo = BeanUtil.mapToBean(getUserMap, UserInfo.class, false);
        if (userInfo != null && userInfo.getId() != null) {
            // 缓存命中
            return userInfo;
        }

        // 2. 缓存未命中，从数据库获取
        try {
            // 从 token 中解析用户ID（需要根据你的 JWT 实现调整）
            Integer userId = jwt.getIdFromToken(accessToken); // 假设你有一个 Jwt 工具类
            System.out.println(userId);
            if (userId == null) {
                return null;
            }

            // 从数据库查询用户信息
            userInfo = userMapper.getUserInfo(userId);
            if (userInfo != null) {
                // 3. 将用户信息存入 Redis
                Map<String, Object> userHash = new HashMap<>();
                userHash.put("id", userInfo.getId().toString());
                userHash.put("username", userInfo.getUsername());
                userHash.put("role", userInfo.getRole().toString());
                userHash.put("avatar", userInfo.getAvatar());
                Map<String, Object> userMap = BeanUtil.beanToMap(userHash);
                stringRedisTemplate.opsForHash().putAll(redisKey, userMap);
                stringRedisTemplate.expire(redisKey, accessTokenExpirationTime, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // 记录异常但不影响业务流程
            System.out.println("获取用户信息失败，token: {}" + request + e);
        }

        return userInfo;
    }
}