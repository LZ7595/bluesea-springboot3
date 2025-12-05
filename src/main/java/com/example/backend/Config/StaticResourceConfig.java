package com.example.backend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${image.storage.directory}")
    private String storageDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 方法1：使用addResourceHandler明确映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + storageDirectory)
                .setCachePeriod(3600);

        // 方法2：同时映射根路径（备用方案）
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + storageDirectory)
                .setCachePeriod(3600);

        // 方法3：确保classpath资源也可访问
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("classpath:/static/");
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            // 确保静态资源处理
            if (factory instanceof TomcatServletWebServerFactory tomcatFactory) {
                // Tomcat特定配置
            }
        };
    }
}