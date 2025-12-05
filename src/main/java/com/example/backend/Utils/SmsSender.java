// com.example.backend.Utils.SmsSender.java
package com.example.backend.Utils;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsSender {
    private static final Logger logger = LoggerFactory.getLogger(SmsSender.class);

    @Value("${aliyun.sms.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.sms.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.sms.sign-name}")
    private String signName;

    @Value("${aliyun.sms.template-id}")
    private String templateId;

    public boolean sendSms(String phoneNumber, String code) {
        try {
            // 配置客户端
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = "dysmsapi.aliyuncs.com";
            Client client = new Client(config);

            // 构建发送短信请求
            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumber)
                    .setSignName(signName)
                    .setTemplateCode(templateId)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            // 发送短信
            SendSmsResponse response = client.sendSms(sendSmsRequest);
            if ("OK".equals(response.getBody().getCode())) {
                logger.info("向手机号 {} 发送验证码: {} 成功", phoneNumber, code);
                return true;
            } else {
                logger.error("向手机号 {} 发送验证码失败，错误码: {}, 错误信息: {}",
                        phoneNumber, response.getBody().getCode(), response.getBody().getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("发送短信时发生异常: {}", e.getMessage());
            return false;
        }
    }
}