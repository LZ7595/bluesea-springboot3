package com.example.backend.Utils;

public class RedisConstants {
    public static final String AUTH_CODE_KEY = "auth:code:";
    public static final int CODE_EXPIRE_TIME = 60 * 5; // 5分钟

    public static final String AUTH_USER_KEY = "auth:user:";

    public static final String REFRESH_TOKEN_KEY = "refresh:token:";

    public static final String PRODUCT_DETAILS_KEY = "product:details:";

    public static final String PRODUCT_PROMOTIONS_KEY = "product:promotions:";


    public static final Long GLOBAL_CACHE_EXPIRE_TIME = 5 * 60L; // 5分钟
    public static final Long SESSION_CACHE_EXPIRE_TIME = 30 * 60L; // 30分钟


}
