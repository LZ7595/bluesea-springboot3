package com.example.backend.Config;

import com.example.backend.Utils.CookieExpirationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CookieExpirationInterceptor cookieExpirationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器
        registry.addInterceptor(cookieExpirationInterceptor)
                .addPathPatterns("/product/details") // 拦截所有请求
                .excludePathPatterns("/auth/**"); // 排除登录和注册接口，可根据实际情况修改
    }
}