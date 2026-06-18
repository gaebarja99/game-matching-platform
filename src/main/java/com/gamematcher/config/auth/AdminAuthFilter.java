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
 * admin.api.key 가 비어있으면 보안을 위해 기본적으로 차단한다 (fail-safe).
 * 로컬 개발 시에는 application-local.properties 등에서 admin.api.key 를 명시적으로 설정할 것.
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

        // admin.api.key가 비어있으면 차단 (운영 환경에서 키 설정을 빠뜨려도 관리자 API가 열리지 않도록 fail-safe)
        if (!StringUtils.hasText(adminApiKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"error\":\"관리자 API 키가 설정되지 않았습니다.\",\"status\":403}");
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
