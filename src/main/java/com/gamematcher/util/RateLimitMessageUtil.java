package com.gamematcher.util;

import org.springframework.web.client.HttpStatusCodeException;

/**
 * 외부 전적 API 등에서 429 / Rate limit 응답이 올 때 사용자에게는 동일한 안내 문구를 쓴다.
 */
public final class RateLimitMessageUtil {

    private RateLimitMessageUtil() {}

    public static final String RATE_LIMIT_USER_MESSAGE =
            "전적 API 요청이 많아 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    public static boolean isRateLimitedThrowable(Throwable t) {
        if (t == null) {
            return false;
        }
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof HttpStatusCodeException h && h.getStatusCode().value() == 429) {
                return true;
            }
            if (looksLikeRateLimitedText(cur.getMessage())) {
                return true;
            }
        }
        return false;
    }

    public static boolean looksLikeRateLimitedText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String u = text.toUpperCase();
        if (u.contains("TOO MANY REQUESTS") || u.contains("TOO_MANY_REQUESTS")) {
            return true;
        }
        if (u.contains("RATE LIMIT")) {
            return true;
        }
        return u.contains("429");
    }

    /**
     * @param rawMessage 사용자에게 보일 원문 (또는 API 본문 일부)
     * @param throwable  cause 체인에 429 예외가 있으면 치환
     */
    public static String toUserMessage(String rawMessage, Throwable throwable) {
        if (isRateLimitedThrowable(throwable) || looksLikeRateLimitedText(rawMessage)) {
            return RATE_LIMIT_USER_MESSAGE;
        }
        return rawMessage;
    }
}
