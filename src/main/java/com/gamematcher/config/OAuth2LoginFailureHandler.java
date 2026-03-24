package com.gamematcher.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * OAuth2 로그인 실패 시 프론트엔드 로그인 페이지로 리다이렉트 (쿼리 파라미터로 오류 전달).
 */
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String redirectUrl = getRedirectTarget(request);
        String errorParam = "error=oauth_failed";
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            errorParam += "&message=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        }
        getRedirectStrategy().sendRedirect(request, response, redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + errorParam);
    }

    private String getRedirectTarget(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String saved = (String) session.getAttribute(OAuth2RedirectOriginFilter.SESSION_OAUTH2_REDIRECT_ORIGIN);
            if (saved != null && !saved.isBlank()) {
                String base = saved.replaceAll("/+$", "");
                return base + "/login";
            }
        }
        String base = getFrontendBaseMatchingRequest(request);
        return base != null ? base + "/login" : "/login";
    }

    /** 요청 Host(127.0.0.1 / localhost)와 일치하는 app.frontend.url 항목 사용 */
    private String getFrontendBaseMatchingRequest(HttpServletRequest request) {
        if (frontendUrl == null || frontendUrl.isBlank()) return null;
        String requestHost = request.getServerName();
        if (requestHost == null || requestHost.isBlank()) return null;
        String[] urls = frontendUrl.split(",");
        for (String u : urls) {
            String base = u.trim().replaceAll("/+$", "");
            if (base.isEmpty()) continue;
            if (base.contains("://" + requestHost + ":") || base.endsWith("://" + requestHost)) {
                return base;
            }
        }
        String first = urls.length > 0 ? urls[0].trim().replaceAll("/+$", "") : "";
        return first.isEmpty() ? null : first;
    }
}
