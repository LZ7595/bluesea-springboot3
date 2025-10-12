package com.example.backend.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 快递 100 接口配置类（读取 application.yml 中的 kuaidi100 配置）
 */
@Component
@ConfigurationProperties(prefix = "kuaidi100") // 对应 yml 中的 "kuaidi100" 前缀
@Data //lombok 注解，自动生成 getter/setter
public class Kuaidi100Config {
    /*
    快递 100 授权码（customer）
    */
    private String customer;
    /**
     * 快递 100 签名密钥（key）
     */
    private String key;
    /**
     * 快递 100 官方查询接口地址
     */
    private String apiUrl;
}