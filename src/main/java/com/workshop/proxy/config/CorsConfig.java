package com.workshop.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String LOCAL_FRONTEND = "http://localhost:5173";

    private final String allowedOrigin;

    public CorsConfig(@Value("${ALLOWED_ORIGIN:}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigin.isBlank()
                ? new String[]{LOCAL_FRONTEND}
                : new String[]{LOCAL_FRONTEND, allowedOrigin};
        registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET", "POST");
    }
}
