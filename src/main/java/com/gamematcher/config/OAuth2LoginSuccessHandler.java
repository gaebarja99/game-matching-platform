package com.gamematcher.config;

import com.gamematcher.constant.Provider;
import com.gamematcher.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String SESSION_USER_ID = "userId";

    private final AuthService authService;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    private String getRedirectUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String saved = (String) session.getAttribute(OAuth2RedirectOriginFilter.SESSION_OAUTH2_REDIRECT_ORIGIN);
            if (saved != null && !saved.isBlank()) {
                session.removeAttribute(OAuth2RedirectOriginFilter.SESSION_OAUTH2_REDIRECT_ORIGIN);
                return saved;
            }
        }
        String base = getFrontendBaseMatchingRequest(request);
        return base != null ? base + "/" : "/";
    }

    /** 세션에 저장된 origin이 없을 때, 요청 Host(127.0.0.1 / localhost)와 일치하는 app.frontend.url 항목 사용 */
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            response.sendRedirect(getRedirectUrl(request));
            return;
        }
        String registrationId = authentication instanceof OAuth2AuthenticationToken token
                ? token.getAuthorizedClientRegistrationId()
                : null;
        String sub;
        String email;
        String name;
        String picture;
        Provider provider;
        if ("kakao".equals(registrationId)) {
            Object idObj = oauth2User.getAttribute("id");
            sub = idObj != null ? String.valueOf(idObj) : null;
            email = null;
            name = null;
            picture = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    name = (String) profile.get("nickname");
                    picture = (String) profile.get("profile_image_url");
                    if (picture == null) picture = (String) profile.get("thumbnail_image_url");
                }
            }
            if (name == null || picture == null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> properties = (Map<String, Object>) oauth2User.getAttribute("properties");
                if (properties != null) {
                    if (name == null) name = (String) properties.get("nickname");
                    if (picture == null) picture = (String) properties.get("profile_image");
                }
            }
            provider = Provider.KAKAO;
        } else if ("naver".equals(registrationId)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> naverResponse = (Map<String, Object>) oauth2User.getAttribute("response");
            if (naverResponse != null) {
                Object idObj = naverResponse.get("id");
                sub = idObj != null ? String.valueOf(idObj) : null;
                email = (String) naverResponse.get("email");
                name = (String) naverResponse.get("nickname");
                if (name == null) name = (String) naverResponse.get("name");
                picture = (String) naverResponse.get("profile_image");
            } else {
                sub = null;
                email = null;
                name = null;
                picture = null;
            }
            provider = Provider.NAVER;
        } else {
            sub = oauth2User.getAttribute("sub");
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            picture = oauth2User.getAttribute("picture");
            if (name == null && oauth2User.getAttributes().get("name") instanceof String s) {
                name = s;
            }
            if (picture == null && oauth2User.getAttributes().get("picture") instanceof String s) {
                picture = s;
            }
            provider = Provider.GOOGLE;
        }
        if (sub == null) {
            response.sendRedirect(getRedirectUrl(request) + "?error=oauth_missing_sub");
            return;
        }
        var authResponse = authService.findOrCreateByOAuth(provider, sub, email, name, picture);
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_ID, authResponse.getId());
        OnlineUserStore.add(authResponse.getId());
        response.sendRedirect(getRedirectUrl(request));
    }
}
