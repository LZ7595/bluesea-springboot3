package com.example.backend.Config;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// 支付宝配置类（支持沙箱/正式环境切换）
@Configuration
public class AlipayConfig {
    // 正式环境配置
    @Value("${alipay.production.appId}")
    private String appId;
    @Value("${alipay.production.privateKey}")
    private String privateKey;
    @Value("${alipay.production.publicKey}")
    private String publicKey;
    @Value("${alipay.production.gatewayUrl}")
    private String gatewayUrl;

    // 沙箱环境配置
    @Value("${alipay.sandbox.appId}")
    private String sandboxAppId;
    @Value("${alipay.sandbox.privateKey}")
    private String sandboxPrivateKey;
    @Value("${alipay.sandbox.publicKey}")
    private String sandboxPublicKey;
    @Value("${alipay.sandbox.gatewayUrl}")
    private String sandboxGatewayUrl;

    // 公共回调地址
    @Value("${alipay.returnUrl}")
    private String returnUrl;
    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    /**
     * 根据环境获取支付宝客户端
     * @param isSandbox 是否为沙箱环境
     */
    public AlipayClient getAlipayClient(boolean isSandbox) {
        if (isSandbox) {
            return new DefaultAlipayClient(
                    sandboxGatewayUrl,
                    sandboxAppId,
                    sandboxPrivateKey,
                    "json",
                    "UTF-8",
                    sandboxPublicKey,
                    "RSA2"
            );
        } else {
            return new DefaultAlipayClient(
                    gatewayUrl,
                    appId,
                    privateKey,
                    "json",
                    "UTF-8",
                    publicKey,
                    "RSA2"
            );
        }
    }

    /**
     * 根据环境获取回调地址
     */
    public String getReturnUrl(boolean isSandbox) {
        return returnUrl;  // 沙箱和正式环境使用相同的回调地址
    }

    public String getNotifyUrl(boolean isSandbox) {
        return notifyUrl;  // 沙箱和正式环境使用相同的通知地址
    }

    // Getter方法（如果需要在其他地方直接获取配置）
    public String getPublicKey(boolean isSandbox) {
        return isSandbox ? sandboxPublicKey : publicKey;
    }

    public String getAppId(boolean isSandbox) {
        return isSandbox ? sandboxAppId : appId;
    }
}
