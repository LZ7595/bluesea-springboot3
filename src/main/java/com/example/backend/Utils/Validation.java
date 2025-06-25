package com.example.backend.Utils;

import java.util.regex.Pattern;

public class Validation {

    // 邮箱验证正则表达式
    private static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    // 电话号码验证正则表达式（这里以中国大陆的手机号码为例）
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 验证邮箱地址是否正确
     *
     * @param email 需要验证的邮箱地址
     * @return 如果邮箱地址正确，返回true；否则返回false
     */
    public static boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        return pattern.matcher(email).matches();
    }

    /**
     * 验证电话号码是否正确
     *
     * @param phone 需要验证的电话号码
     * @return 如果电话号码正确，返回true；否则返回false
     */
    public static boolean isValidPhone(String phone) {
        Pattern pattern = Pattern.compile(PHONE_REGEX);
        return pattern.matcher(phone).matches();
    }
}
