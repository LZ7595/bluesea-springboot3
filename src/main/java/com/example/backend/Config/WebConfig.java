package com.example.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import com.example.backend.Utils.JwtInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    // 通过构造函数注入 JwtInterceptor
    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 方法1：使用addAllowedOriginPattern（推荐，支持通配符）
        // 允许本地开发环境（H5）
        config.addAllowedOriginPattern("http://localhost:5173");
        // 允许生产环境域名（示例）
        config.addAllowedOriginPattern("http://192.168.1.100");
        // 允许另一个域名（示例）
        config.addAllowedOriginPattern("http://localhost:5555");
        // 允许所有子域名（如 *.example.com）
        // config.addAllowedOriginPattern("https://*.example.com");

        // 方法2：多次调用addAllowedOrigin（不推荐，不支持通配符，且Spring新版本可能废弃）
        // config.addAllowedOrigin("http://localhost:5173");
        // config.addAllowedOrigin("https://your-production-domain.com");

        // 允许所有请求方法（GET/POST/PUT等）
        config.addAllowedMethod("*");
        // 允许所有请求头（包括自定义头，如Token）
        config.addAllowedHeader("*");
        // 允许携带Cookie（如果需要认证）
        config.setAllowCredentials(true);
        // 预检请求有效期（避免频繁预检）
        config.setMaxAge(3600L);

        // 应用到所有接口路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/sendCode",
                        "/public/**",
                        "/static/**",
                        "/product/**",
                        "/review/product/**",
                        "/categoryBrand/**",
                        "/order/pay/notify"
                );
    }
}
