package com.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS配置 - 支持前后端分离开发
 * 允许前端(Vue)应用跨域访问后端API
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许的源(开发环境)
        config.addAllowedOrigin("http://localhost:3000");  // Vue开发服务器
        config.addAllowedOrigin("http://127.0.0.1:3000");
        
        // 生产环境可以配置具体域名
        // config.addAllowedOrigin("https://your-domain.com");
        
        // 或者允许所有源(仅用于开发环境,生产环境不推荐)
        // config.addAllowedOriginPattern("*");

        // 允许的HTTP方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // 允许的请求头
        config.addAllowedHeader("*");

        // 允许发送Cookie
        config.setAllowCredentials(true);

        // 预检请求的有效期(秒)
        config.setMaxAge(3600L);

        // 暴露的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Type");

        // 对所有路径应用配置
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}

