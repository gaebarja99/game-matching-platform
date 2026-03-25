package com.gamematcher.config.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 관리자 API(/api/admin/**) 접근 시 X-Admin-Key 헤더 검증
 * 운영환경에서는 application.properties의 admin.api.key 설정 필수
 */
@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    @Value("${admin.api.key:}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // admin.api.key가 비어있으면 개발 모드 (허용)
        if (!StringUtils.hasText(adminApiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader("X-Admin-Key");
        if (adminApiKey.equals(providedKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"error\":\"관리자 권한이 필요합니다.\",\"status\":403}");
        }
    }
}
