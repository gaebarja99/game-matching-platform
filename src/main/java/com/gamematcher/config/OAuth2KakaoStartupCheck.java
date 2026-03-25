package com.gamematcher.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

/**
 * oauth 프로필 기동 시 카카오 client-secret 로드 여부를 로그로 확인.
 * 401 invalid_token_response 시 시크릿/리다이렉트 URI/호출 허용 IP 점검용.
 */
@Component
@Profile("oauth")
public class OAuth2KakaoStartupCheck {

    private static final Logger log = LoggerFactory.getLogger(OAuth2KakaoStartupCheck.class);

    private final ClientRegistrationRepository registrationRepository;

    public OAuth2KakaoStartupCheck(ClientRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            ClientRegistration kakao = registrationRepository.findByRegistrationId("kakao");
            if (kakao != null) {
                String secret = kakao.getClientSecret();
                boolean hasSecret = secret != null && !secret.isBlank();
                log.info("[OAuth2 Kakao] client-secret configured: {} (On 401: check Kakao console 'Allowed IPs' and redirect URI)",
                        hasSecret ? "yes" : "NO - check application-oauth-local.properties or KAKAO_CLIENT_SECRET");
            }
            ClientRegistration naver = registrationRepository.findByRegistrationId("naver");
            if (naver != null) {
                String cid = naver.getClientId();
                boolean configured = cid != null && !cid.isBlank() && !"not-configured".equalsIgnoreCase(cid);
                log.info("[OAuth2 Naver] client-id configured: {} (If not set, Naver login redirects back to login. Check application-oauth-local.properties or same file at project root)",
                        configured ? "yes" : "NO");
            }
        } catch (Exception e) {
            log.warn("[OAuth2] startup check failed: {}", e.getMessage());
        }
    }
}
