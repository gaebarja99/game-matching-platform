package com.gamematcher.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * reCAPTCHA Enterprise assessment API를 호출하여 클라이언트 토큰을 검증합니다.
 * <p>
 * 설정: application.properties의 app.recaptcha.*
 * API 문서: https://cloud.google.com/recaptcha-enterprise/docs/assessments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecaptchaEnterpriseService {

    private static final String ASSESSMENTS_URL = "https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments?key=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.recaptcha.enabled:false}")
    private boolean enabled;

    @Value("${app.recaptcha.api-key:}")
    private String apiKey;

    @Value("${app.recaptcha.project-id:gamematcher-ddec4}")
    private String projectId;

    @Value("${app.recaptcha.site-key:}")
    private String siteKey;

    /**
     * 토큰이 비어 있으면 검증하지 않고 true (미사용 시 통과).
     * enabled이고 토큰이 있으면 assessment API로 검증 후 tokenProperties.valid 및 점수 기준으로 결과 반환.
     *
     * @param token       grecaptcha.enterprise.execute()에서 받은 토큰 (null/blank면 검증 스킵)
     * @param expectedAction execute 시 사용한 action (예: FIND_LOGIN_ID, RESET_PASSWORD)
     * @return 검증 통과 여부
     */
    public boolean verify(String token, String expectedAction) {
        if (token == null || token.isBlank()) {
            return true;
        }
        if (!enabled || apiKey == null || apiKey.isBlank() || siteKey == null || siteKey.isBlank()) {
            log.debug("reCAPTCHA verification skipped: disabled or missing config");
            return true;
        }
        try {
            String url = String.format(ASSESSMENTS_URL, projectId, apiKey);
            Map<String, Object> event = new java.util.HashMap<>();
            event.put("token", token.trim());
            event.put("siteKey", siteKey.trim());
            if (expectedAction != null && !expectedAction.isBlank()) {
                event.put("expectedAction", expectedAction.trim());
            }
            Map<String, Object> body = java.util.Collections.singletonMap("event", event);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode tokenProps = root.path("tokenProperties");
                boolean valid = tokenProps.path("valid").asBoolean(false);
                if (!valid) {
                    String reason = tokenProps.path("invalidReason").asText("");
                    log.warn("reCAPTCHA token invalid: {}", reason);
                    return false;
                }
                JsonNode risk = root.path("riskAnalysis");
                double score = risk.path("score").asDouble(0.0);
                // 0.0 = 높은 위험, 1.0 = 낮은 위험. 임계값 0.5 미만이면 봇 가능성으로 거부 가능.
                double minScore = 0.3;
                if (score < minScore) {
                    log.warn("reCAPTCHA risk score too low: {} (min {})", score, minScore);
                    return false;
                }
                return true;
            }
            log.warn("reCAPTCHA assessment non-2xx: {}", response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("reCAPTCHA assessment request failed", e);
            return false;
        } catch (Exception e) {
            log.error("reCAPTCHA assessment parse failed", e);
            return false;
        }
    }
}
