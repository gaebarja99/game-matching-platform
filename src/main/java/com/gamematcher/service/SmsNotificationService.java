package com.gamematcher.service;

import com.gamematcher.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationService {

    private final SmsProperties smsProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isEnabled() {
        return smsProperties.isEnabled();
    }

    public void sendSms(String toPhone, String message) {
        if (!isEnabled()) return;
        if (toPhone == null || toPhone.isBlank()) return;
        if (message == null || message.isBlank()) return;

        String provider = safe(smsProperties.getProvider()).toLowerCase();
        if ("solapi".equals(provider)) {
            sendViaSolapi(toPhone, message);
            return;
        }
        if ("naver-sens".equals(provider)) {
            sendViaNaverSens(toPhone, message);
        }
    }

    public void sendPaymentSms(String toPhone, String message) {
        sendSms(toPhone, message);
    }

    private void sendViaNaverSens(String toPhone, String message) {
        if (toPhone == null || toPhone.isBlank()) return;

        String serviceId = safe(smsProperties.getServiceId());
        String accessKey = safe(smsProperties.getAccessKey());
        String secretKey = safe(smsProperties.getSecretKey());
        String fromNumber = normalizePhone(safe(smsProperties.getFromNumber()));
        String to = normalizePhone(toPhone);
        if (serviceId.isBlank() || accessKey.isBlank() || secretKey.isBlank() || fromNumber.isBlank() || to.isBlank()) {
            log.warn("SMS disabled by missing config/value. serviceId? {}, from? {}, to? {}",
                    !serviceId.isBlank(), !fromNumber.isBlank(), !to.isBlank());
            return;
        }

        String uri = "/sms/v2/services/" + serviceId + "/messages";
        String url = safe(smsProperties.getBaseUrl()) + uri;
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature;
        try {
            signature = makeSignature(secretKey, "POST", uri, timestamp, accessKey);
        } catch (Exception e) {
            log.warn("SMS signature creation failed", e);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-ncp-apigw-timestamp", timestamp);
        headers.set("x-ncp-iam-access-key", accessKey);
        headers.set("x-ncp-apigw-signature-v2", signature);

        Map<String, Object> body = Map.of(
                "type", "SMS",
                "contentType", "COMM",
                "countryCode", safe(smsProperties.getCountryCode(), "82"),
                "from", fromNumber,
                "content", message,
                "messages", List.of(Map.of("to", to, "content", message))
        );

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.warn("SMS send failed: to={}", mask(to), e);
        }
    }

    private void sendViaSolapi(String toPhone, String message) {
        String accessKey = safe(smsProperties.getAccessKey());
        String secretKey = safe(smsProperties.getSecretKey());
        String fromNumber = normalizePhone(safe(smsProperties.getFromNumber()));
        String to = normalizePhone(toPhone);
        if (accessKey.isBlank() || secretKey.isBlank() || fromNumber.isBlank() || to.isBlank()) {
            log.warn("SOLAPI SMS disabled by missing config/value. apiKey? {}, from? {}, to? {}",
                    !accessKey.isBlank(), !fromNumber.isBlank(), !to.isBlank());
            return;
        }

        String date = Instant.now().toString();
        String salt = randomSalt(24);
        String signature;
        try {
            signature = makeSolapiSignature(secretKey, date, salt);
        } catch (Exception e) {
            log.warn("SOLAPI SMS signature creation failed", e);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "HMAC-SHA256 apiKey=" + accessKey + ", date=" + date + ", salt=" + salt + ", signature=" + signature);

        Map<String, Object> body = Map.of(
                "message", Map.of(
                        "to", to,
                        "from", fromNumber,
                        "text", message
                )
        );

        String baseUrl = safe(smsProperties.getSolapiBaseUrl(), "https://api.solapi.com").replaceAll("/+$", "");
        String url = baseUrl + "/messages/v4/send";

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.warn("SOLAPI SMS send failed: to={}", mask(to), e);
        }
    }

    private String makeSignature(String secretKey, String method, String uri, String timestamp, String accessKey) throws Exception {
        String payload = method + " " + uri + "\n" + timestamp + "\n" + accessKey;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private String makeSolapiSignature(String secretKey, String date, String salt) throws Exception {
        String payload = date + salt;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private String randomSalt(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return raw.length() > length ? raw.substring(0, length) : raw;
    }

    private String normalizePhone(String v) {
        if (v == null) return "";
        return v.replaceAll("[^0-9]", "");
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private String safe(String v, String fallback) {
        String s = safe(v);
        return s.isBlank() ? fallback : s;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 8) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
