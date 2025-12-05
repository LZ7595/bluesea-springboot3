package com.example.backend.Utils;
import cn.hutool.core.bean.BeanUtil;
import com.example.backend.Model.Vo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// utils/SensitiveInfo.java
@Component
@Slf4j
public class SensitiveInfo {

    /**
     * 邮箱脱敏
     */
    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }

        try {
            String trimmedEmail = email.trim();
            int atIndex = trimmedEmail.indexOf('@');
            if (atIndex <= 0) {
                return "***"; // 无效邮箱格式
            }

            String localPart = trimmedEmail.substring(0, atIndex);
            String domain = trimmedEmail.substring(atIndex);

            if (localPart.length() <= 1) {
                return "***" + domain;
            } else if (localPart.length() <= 3) {
                return localPart.charAt(0) + "***" + domain;
            } else {
                return localPart.substring(0, 3) + "***" + domain;
            }
        } catch (Exception e) {
            log.warn("邮箱脱敏失败: {}", email, e);
            return "***";
        }
    }

    /**
     * 手机号脱敏
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }

        try {
            String trimmedPhone = phone.trim();
            if (trimmedPhone.length() == 11) {
                // 标准11位手机号：138****5678
                return trimmedPhone.substring(0, 3) + "****" + trimmedPhone.substring(7);
            } else if (trimmedPhone.length() > 7) {
                // 长号码：显示前3位和后4位
                return trimmedPhone.substring(0, 3) + "****" + trimmedPhone.substring(trimmedPhone.length() - 4);
            } else if (trimmedPhone.length() > 4) {
                // 较短号码：显示前2位和后2位
                int prefixLength = Math.min(2, trimmedPhone.length() - 2);
                return trimmedPhone.substring(0, prefixLength) + "****" + trimmedPhone.substring(trimmedPhone.length() - 2);
            } else {
                // 很短号码：全部隐藏
                return "****";
            }
        } catch (Exception e) {
            log.warn("手机号脱敏失败: {}", phone, e);
            return "****";
        }
    }

    /**
     * 身份证号脱敏
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return idCard;
        }

        try {
            String trimmedIdCard = idCard.trim();
            if (trimmedIdCard.length() == 18) {
                return trimmedIdCard.substring(0, 6) + "********" + trimmedIdCard.substring(14);
            } else if (trimmedIdCard.length() == 15) {
                return trimmedIdCard.substring(0, 6) + "******" + trimmedIdCard.substring(12);
            } else {
                return "***************";
            }
        } catch (Exception e) {
            log.warn("身份证号脱敏失败: {}", idCard, e);
            return "***************";
        }
    }

    /**
     * 银行卡号脱敏
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.trim().isEmpty()) {
            return bankCard;
        }

        try {
            String trimmedBankCard = bankCard.trim();
            if (trimmedBankCard.length() >= 16) {
                return trimmedBankCard.substring(0, 6) + "******" + trimmedBankCard.substring(trimmedBankCard.length() - 4);
            } else if (trimmedBankCard.length() >= 10) {
                return trimmedBankCard.substring(0, 4) + "****" + trimmedBankCard.substring(trimmedBankCard.length() - 4);
            } else {
                return "****" + trimmedBankCard.substring(Math.max(0, trimmedBankCard.length() - 4));
            }
        } catch (Exception e) {
            log.warn("银行卡号脱敏失败: {}", bankCard, e);
            return "************";
        }
    }

    /**
     * 姓名脱敏
     */
    public static String maskRealName(String realName) {
        if (realName == null || realName.trim().isEmpty()) {
            return realName;
        }

        try {
            String trimmedName = realName.trim();
            if (trimmedName.length() == 1) {
                return "*";
            } else if (trimmedName.length() == 2) {
                return trimmedName.charAt(0) + "*";
            } else {
                return trimmedName.charAt(0) + "*" + trimmedName.charAt(trimmedName.length() - 1);
            }
        } catch (Exception e) {
            log.warn("姓名脱敏失败: {}", realName, e);
            return "**";
        }
    }

    /**
     * 对用户敏感信息进行脱敏处理
     */
    public UserInfo maskSensitiveInfo(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }

        // 创建脱敏后的用户信息副本
        UserInfo maskedUser = new UserInfo();
        BeanUtil.copyProperties(userInfo, maskedUser);

        // 邮箱脱敏：显示前3位和域名，中间用***代替
        if (maskedUser.getEmail() != null && !maskedUser.getEmail().isEmpty()) {
            maskedUser.setEmail(maskEmail(maskedUser.getEmail()));
        }

        // 手机号脱敏：显示前3位和后4位，中间用****代替
        if (maskedUser.getPhone() != null && !maskedUser.getPhone().isEmpty()) {
            maskedUser.setPhone(maskPhone(maskedUser.getPhone()));
        }

        return maskedUser;
    }
}