package com.gamematcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 분리형 아키텍처: React(Vite) 등 별도 프론트엔드에서 API 호출 시 CORS 허용.
 * <ul>
 *   <li>{@code app.frontend.url} — 배포/팀 공용 프론트 주소(쉼표 구분)</li>
 *   <li>{@code app.frontend.base-url} — 로컬 개발 기본값 {@code http://localhost:5173} 등(OAuth·CORS 공통)</li>
 * </ul>
 * 예전에는 {@code app.frontend.url}만 쓰면 로컬 5173이 빠져 프리플라이트가 실패했음.
 */
@Configuration
public class CorsConfig {

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private static List<String> parseOriginList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(s -> s.trim().replaceAll("/+$", ""))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        Set<String> origins = new LinkedHashSet<>();
        parseOriginList(frontendUrl).forEach(origins::add);
        parseOriginList(frontendBaseUrl).forEach(origins::add);
        /* Vite를 127.0.0.1 로 열면 Origin 이 localhost 와 달라 CORS 가 막힘 */
        if (origins.contains("http://localhost:5173")) {
            origins.add("http://127.0.0.1:5173");
        }

        if (!origins.isEmpty()) {
            config.setAllowedOrigins(new ArrayList<>(origins));
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
