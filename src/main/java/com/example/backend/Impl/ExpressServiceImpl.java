package com.example.backend.Impl;
import com.alibaba.fastjson.JSONObject;
import com.example.backend.Config.Kuaidi100Config;
import com.example.backend.Entity.ExpressRequest;
import com.example.backend.Service.ExpressService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ExpressServiceImpl implements ExpressService {
    @Autowired
    private Kuaidi100Config kuaidiConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Redis缓存键前缀
    private static final String EXPRESS_CACHE_KEY = "express:info:";
    // 缓存过期时间(分钟)
    private static final long CACHE_EXPIRE_MINUTES = 60;
    // 空结果缓存过期时间(分钟)
    private static final long EMPTY_CACHE_EXPIRE_MINUTES = 5;

    /**
     * 生成快递100签名
     */
    private String generateSign(Map<String, Object> param) {
        String paramStr = JSONObject.toJSONString(param);  // 转换为无空格JSON
        String signRaw = paramStr + kuaidiConfig.getKey() + kuaidiConfig.getCustomer();
        return DigestUtils.md5Hex(signRaw).toUpperCase();  // MD5加密并转大写
    }

    /**
     * 查询快递信息 (带Redis缓存实现)
     */
    @Override
    public ResponseEntity<?> queryExpress(ExpressRequest request) {
        try {
            log.info("queryExpress: com={}, num={}", request.getCom(), request.getNum());

            // 1. 构建缓存键 (快递公司+快递单号作为唯一标识)
            String cacheKey = EXPRESS_CACHE_KEY + request.getCom().toLowerCase() + ":" + request.getNum().trim();

            // 2. 尝试从Redis获取缓存
            String cachedResult = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                // 检查是否是空结果标记
                if ("{}".equals(cachedResult)) {
                    JSONObject error = new JSONObject();
                    error.put("errorCode", "NOT_FOUND");
                    error.put("errorMessage", "快递信息不存在或已过期");
                    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
                }

                // 缓存命中，直接返回
                JSONObject result = JSONObject.parseObject(cachedResult);
                return new ResponseEntity<>(result, HttpStatus.OK);
            }

            // 3. 缓存未命中：调用接口查询
            // 3.1 构建请求参数
            Map<String, Object> param = new HashMap<>();
            param.put("com", request.getCom().toLowerCase());  // 转为小写
            param.put("num", request.getNum().trim());
            param.put("phone", request.getPhone() == null ? "" : request.getPhone().trim());
            param.put("from", request.getFrom() == null ? "" : request.getFrom().trim());
            param.put("to", request.getTo() == null ? "" : request.getTo().trim());
            param.put("resultv2", "4");  // 开启高级状态解析
            param.put("show", "0");      // 返回JSON格式

            // 3.2 生成签名
            String sign = generateSign(param);

            // 3.3 构建表单请求
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("customer", kuaidiConfig.getCustomer());
            formData.add("sign", sign);
            formData.add("param", JSONObject.toJSONString(param));

            // 3.4 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            // 3.5 调用快递100接口
            ResponseEntity<String> response = restTemplate.postForEntity(
                    kuaidiConfig.getApiUrl(),
                    requestEntity,
                    String.class
            );

            // 3.6 解析原始响应
            JSONObject result = JSONObject.parseObject(response.getBody());

            // 4. 处理响应结果并缓存
            if ("200".equals(result.getString("status")) || "ok".equals(result.getString("message"))) {
                // 成功：缓存结果并返回
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        result.toJSONString(),
                        CACHE_EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );
                return new ResponseEntity<>(result, HttpStatus.OK);
            } else {
                // 业务错误：缓存空结果避免重复查询
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        "{}",
                        EMPTY_CACHE_EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );

                JSONObject error = new JSONObject();
                error.put("errorCode", result.getString("returnCode") != null ? result.getString("returnCode") : "ERROR");
                error.put("errorMessage", result.getString("message") != null ? result.getString("message") : "查询失败");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error("查询快递信息异常", e);
            // 服务器异常：不缓存异常结果
            JSONObject error = new JSONObject();
            error.put("errorCode", "SERVER_ERROR");
            error.put("errorMessage", "服务器异常：" + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 清除指定快递单号的缓存
     */
    public void clearExpressCache(String com, String num) {
        String cacheKey = EXPRESS_CACHE_KEY + com.toLowerCase() + ":" + num.trim();
        stringRedisTemplate.delete(cacheKey);
        log.info("已清除快递缓存: {}", cacheKey);
    }
}
