package com.gamematcher.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 카카오 OAuth: client-id 없으면 KOE101 방지, client-secret 없으면 콜백 후 토큰 401(invalid_token_response) 방지.
 */
@Component
public class OAuth2KakaoNotConfiguredFilter extends OncePerRequestFilter {

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id:not-configured}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("/oauth2/authorization/kakao".equals(request.getRequestURI()) && isGet(request)) {
            String clientId = kakaoClientId != null ? kakaoClientId.trim() : "";
            if (clientId.isEmpty() || "not-configured".equalsIgnoreCase(clientId)) {
                String redirect = buildRedirectToLogin("oauth_not_configured");
                response.sendRedirect(redirect);
                return;
            }
            String secret = kakaoClientSecret != null ? kakaoClientSecret.trim() : "";
            if (secret.isEmpty()) {
                response.sendRedirect(buildRedirectToLogin("kakao_secret_required"));
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
