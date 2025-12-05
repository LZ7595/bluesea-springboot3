package com.example.backend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import com.example.backend.Utils.JwtInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
        config.addAllowedOriginPattern("http://192.168.1.102");
        // 允许另一个域名（示例）
        config.addAllowedOriginPattern("http://localhost:5555");
        // 允许所有子域名（如 *.example.com）
        // config.addAllowedOriginPattern("https://*.example.com");

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
    // HTTP请求客户端配置
    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

    @Value("${image.storage.directory}")
    private String imageStorageDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射静态资源路径
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + imageStorageDirectory)
                .setCachePeriod(3600);

        // 如果需要，也可以添加classpath资源的映射
        registry.addResourceHandler("/static/public/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
    @Bean
    public ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 连接超时5秒
        factory.setReadTimeout(10000);   // 读取超时10秒
        return factory;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/address/**","/users/**","/message/**","/order/**","/shoppingCart/**")
                .excludePathPatterns(
                        "/express/**",
                        "/back/**",
                        "/auth/**",
                        "/public/**",
                        "/static/**",
                        "/product/**",
                        "/review/product/**",
                        "/categoryBrand/**",
                        "/order/pay/notify"
                );
    }
}
