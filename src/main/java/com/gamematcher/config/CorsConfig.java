package com.gamematcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 분리형 아키텍처: React 등 별도 프론트엔드에서 API 호출 시 CORS 허용.
 * app.frontend.url 이 설정된 경우에만 해당 Origin 허용 및 credentials 허용.
 */
@Configuration
public class CorsConfig {

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            List<String> origins = java.util.Arrays.stream(frontendUrl.split(","))
                    .map(s -> s.trim().replaceAll("/+$", ""))
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (!origins.isEmpty()) {
                config.setAllowedOrigins(origins);
            }
        }
        if (config.getAllowedOrigins() != null && !config.getAllowedOrigins().isEmpty()) {
            config.setAllowCredentials(true);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setMaxAge(3600L);
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
