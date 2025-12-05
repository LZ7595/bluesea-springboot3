package com.example.backend.Utils;

import java.util.regex.Pattern;

public class Validation {

    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * 验证手机号格式
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        // 简单的手机号验证（1开头，11位数字）
        String phoneRegex = "^1[3-9]\\d{9}$";
        return phone.matches(phoneRegex);
    }

    /**
     * 验证验证码格式
     */
    public static boolean isValidCode(String code) {
        if (code == null) return false;
        // 6位数字验证码
        String codeRegex = "^\\d{6}$";
        return code.matches(codeRegex);
    }
}
