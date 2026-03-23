package com.gamematcher.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * OAuth2가 설정되지 않았을 때만 동작.
 * /oauth2/authorization/google 또는 /kakao 접근 시 프론트엔드 로그인 페이지로 리다이렉트.
 */
@Controller
@RequestMapping("/oauth2/authorization")
@ConditionalOnMissingBean(ClientRegistrationRepository.class)
public class OAuth2NotConfiguredController {

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    private String redirectToLogin() {
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            String first = frontendUrl.split(",")[0].trim().replaceAll("/+$", "");
            if (!first.isEmpty()) {
                return "redirect:" + first + "/login?error=oauth_not_configured";
            }
        }
        return "redirect:/?oauth2_error=not_configured";
    }

    @GetMapping("/google")
    public String googleRedirect() {
        return redirectToLogin();
    }

    @GetMapping("/kakao")
    public String kakaoRedirect() {
        return redirectToLogin();
    }
}
