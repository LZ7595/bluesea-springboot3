package com.example.backend.Utils;

public class RedisConstants {
    public static final String AUTH_CODE_KEY = "auth:code:";
    public static final int CODE_EXPIRE_TIME = 60 * 5; // 5分钟

    public static final String AUTH_USER_KEY = "auth:user:";

    public static final String REFRESH_TOKEN_KEY = "refresh:token:";
}
