package com.example.backend.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import com.example.backend.Model.Enum.ErrorType;
import com.example.backend.Model.Entity.User;
import com.example.backend.Model.Vo.UserInfo;
import com.example.backend.Model.Vo.UserSecurity;
import com.example.backend.Utils.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.beans.BeanMap;
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

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SensitiveInfo sensitiveInfo;
    @Resource
    private Jwt jwt;

    @Autowired
    private Common common;

    @Autowired
    private VerificationCodeUtil verificationCodeUtil;

    @Autowired
    private Encryption encryption;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpirationTime;


    // 根据用户 ID 获取用户综合信息
    public Optional<UserInfo> getUserInfoById(Integer userId) {
        return userMapper.getUserInfoById(userId);
    }

    public ResponseEntity<?> updateUserInfo(Integer userId, String username, String gender, Date birthday, String avatar, HttpServletRequest request) {
        try {
            // 1. 更新数据库
            userMapper.updateUsername(userId, username);
            userMapper.updateBirthday(userId, new java.sql.Date(birthday.getTime()));
            userMapper.updateGender(userId, gender);
            userMapper.updateAvatar(userId, avatar);

            // 2. 精确更新缓存（通过accessToken）
            boolean cacheUpdated = updateUserCacheByToken(userId, username, gender, birthday, avatar, request);

            if (!cacheUpdated) {
                logger.warn("缓存更新失败，但数据库已更新，userId: {}", userId);
            }

            return ResponseEntity.ok("修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("修改失败");
        }
    }

    private boolean updateUserCacheByToken(Integer userId, String username, String gender, Date birthday, String avatar, HttpServletRequest request) {
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
            if (username != null) updates.put("username", username);
            if (gender != null) updates.put("gender", gender);
            if (birthday != null) updates.put("birthday", birthday.toString());
            if (avatar != null) updates.put("avatar", avatar);

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

    public ResponseEntity<?> getSecurityInfo(Integer userId) {
        try {
            UserSecurity userSecurityInfo = userMapper.getSecurityInfo(userId);
            return ResponseEntity.ok().body(userSecurityInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取失败");
        }
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
        // 1. 根据客户端类型提取 accessToken（核心修改）
        String accessToken = common.extractAccessTokenByClient(request);
        if (accessToken == null) {
            return null; // 或抛出未授权异常
        }

        String redisKey = AUTH_USER_KEY + accessToken;

        // 2. 尝试从 Redis 获取用户信息（原有逻辑保留）
        Map<Object, Object> getUserMap = stringRedisTemplate.opsForHash().entries(redisKey);
        UserInfo userInfo = BeanUtil.mapToBean(getUserMap, UserInfo.class, false);
        if (userInfo != null && userInfo.getId() != null) {
            // 缓存命中，直接返回
            return sensitiveInfo.maskSensitiveInfo(userInfo);
        }

        // 3. 缓存未命中，从数据库获取（适配多端 Token 解析）
        try {
            // 从 token 中解析用户ID（JWT工具类需兼容所有端的 Token 格式）
            Integer userId = jwt.getIdFromToken(accessToken);
            if (userId == null) {
                return null;
            }

            // 从数据库查询用户信息
            userInfo = userMapper.getUserInfo(userId);
            if (userInfo != null) {
                UserInfo maskedUserInfo = sensitiveInfo.maskSensitiveInfo(userInfo);
                // 1. 将userInfo对象转为BeanMap（自动映射所有属性）
                BeanMap beanMap = BeanMap.create(maskedUserInfo);

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

                // 4. 存入Redis（原有逻辑不变）
                stringRedisTemplate.opsForHash().putAll(redisKey, userHash);
                stringRedisTemplate.expire(redisKey, accessTokenExpirationTime, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("获取用户信息失败，token: {}", accessToken, e); // 替换System.out为日志
        }

        return userInfo;
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
}