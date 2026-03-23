package com.gamematcher.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 네이버 OAuth client-id가 미설정일 때 /oauth2/authorization/naver 요청을
 * 네이버로 보내지 않고 프론트엔드 로그인 페이지로 리다이렉트하여 오류를 방지.
 */
@Component
public class OAuth2NaverNotConfiguredFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OAuth2NaverNotConfiguredFilter.class);

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Value("${spring.security.oauth2.client.registration.naver.client-id:not-configured}")
    private String naverClientId;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("/oauth2/authorization/naver".equals(request.getRequestURI()) && isGet(request)) {
            String clientId = naverClientId != null ? naverClientId.trim() : "";
            log.info("[Naver OAuth] client-id configured: {}", clientId.isEmpty() || "not-configured".equalsIgnoreCase(clientId) ? "(empty or not-configured)" : "yes (length=" + clientId.length() + ")");
            if (clientId.isEmpty() || "not-configured".equalsIgnoreCase(clientId)) {
                String redirect = buildRedirectToLogin("oauth_not_configured");
                response.sendRedirect(redirect);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isGet(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod());
    }

    private String buildRedirectToLogin(String error) {
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            String base = frontendUrl.split(",")[0].trim().replaceAll("/+$", "");
            if (!base.isEmpty()) {
                return base + "/login?error=" + error;
            }
        }
        return "/?oauth2_error=" + error;
    }
}
