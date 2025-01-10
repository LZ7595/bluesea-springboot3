package com.example.backend.Utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Encryption {
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 私有化构造函数，防止实例化
    private Encryption() {
    }

    // 对密码进行加密
    public static String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    // 验证密码是否匹配
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
