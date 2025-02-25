package com.example.backend.Utils;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmsSender {
    private static final Logger logger = LoggerFactory.getLogger(SmsSender.class);
    // 阿里云 AccessKey ID
    private static final String ACCESS_KEY_ID = "your-access-key-id";
    // 阿里云 AccessKey Secret
    private static final String ACCESS_KEY_SECRET = "your-access-key-secret";
    // 短信签名名称
    private static final String SIGN_NAME = "your-sign-name";
    // 短信模板 ID
    private static final String TEMPLATE_ID = "your-template-id";

    public static boolean sendSms(String phoneNumber, String code) {
        try {
            // 配置客户端
            Config config = new Config()
                    .setAccessKeyId(ACCESS_KEY_ID)
                    .setAccessKeySecret(ACCESS_KEY_SECRET);
            config.endpoint = "dysmsapi.aliyuncs.com";
            Client client = new Client(config);

            // 构建发送短信请求
            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumber)
                    .setSignName(SIGN_NAME)
                    .setTemplateCode(TEMPLATE_ID)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            // 发送短信
            SendSmsResponse response = client.sendSms(sendSmsRequest);
            if ("OK".equals(response.getBody().getCode())) {
                logger.info("向手机号 {} 发送验证码: {} 成功", phoneNumber, code);
                return true;
            } else {
                logger.error("向手机号 {} 发送验证码失败，错误码: {}, 错误信息: {}", phoneNumber, response.getBody().getCode(), response.getBody().getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("发送短信时发生异常: {}", e.getMessage());
            return false;
        }
    }
}