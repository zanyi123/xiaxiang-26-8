package org.example.xiaxiang.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 Web 配置类
 * 负责：跨域 CORS 配置 + 后台访问鉴权拦截
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("[WebConfig] 注册全局 CORS 配置：允许所有 Origin");
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("[WebConfig] 注册后台鉴权拦截器：拦截 /admin/**");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .order(0);
    }
}
