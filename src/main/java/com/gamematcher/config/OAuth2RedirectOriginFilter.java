package com.gamematcher.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OAuth2 진입 요청 시 Referer의 origin을 세션에 저장.
 * 로그인 성공 후 같은 호스트(예: 127.0.0.1:5173)로 리다이렉트하기 위함.
 */
@Component
@Order(-100)
public class OAuth2RedirectOriginFilter extends OncePerRequestFilter {

    public static final String SESSION_OAUTH2_REDIRECT_ORIGIN = "oauth2.redirect.origin";

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getMethod().equals("GET") && request.getRequestURI().startsWith("/oauth2/authorization/")) {
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isBlank() && frontendUrl != null && !frontendUrl.isBlank()) {
                String origin = toOrigin(referer);
                List<String> allowed = Arrays.stream(frontendUrl.split(","))
                        .map(s -> s.trim().replaceAll("/+$", ""))
                        .filter(s -> !s.isEmpty())
                        .map(OAuth2RedirectOriginFilter::toOrigin)
                        .collect(Collectors.toList());
                if (origin != null && allowed.stream().anyMatch(origin::equalsIgnoreCase)) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute(SESSION_OAUTH2_REDIRECT_ORIGIN, origin + "/");
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String toOrigin(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            int end = url.indexOf('/', url.indexOf("//") + 2);
            return end > 0 ? url.substring(0, end) : url;
        } catch (Exception e) {
            return null;
        }
    }
}
