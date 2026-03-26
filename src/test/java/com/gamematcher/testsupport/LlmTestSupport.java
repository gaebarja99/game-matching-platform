package com.gamematcher.testsupport;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * LLM 출력용 수동 테스트에서 API 키·모델을 가져올 때 사용.
 * <p>우선순위: {@code AI_API_KEY} → {@code OPENAI_API_KEY} → {@code application.properties} 의 {@code ai.llm.*}
 * (파일은 클래스패스 또는 {@code src/main/resources/application.properties}).
 */
public final class LlmTestSupport {

    private LlmTestSupport() {
    }

    /**
     * {@code ${ENV:default}} 형태면 콜론 뒤 기본값만, 아니면 원문 그대로.
     */
    static String unwrapSpringDefault(String raw) {
        if (raw == null) {
            return "";
        }
        raw = raw.trim();
        if (raw.startsWith("${") && raw.endsWith("}")) {
            String inner = raw.substring(2, raw.length() - 1);
            int c = inner.indexOf(':');
            if (c >= 0) {
                return inner.substring(c + 1).trim();
            }
            return "";
        }
        return raw;
    }

    private static Properties loadApplicationProperties() {
        Properties p = new Properties();
        try (InputStream in = LlmTestSupport.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                p.load(in);
                return p;
            }
        } catch (Exception ignored) {
            // fall through
        }
        try {
            Path path = Path.of("src/main/resources/application.properties");
            if (Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    p.load(in);
                }
                return p;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return p;
    }

    /**
     * 테스트용 API 키. 환경 변수가 비어 있지 않으면 항상 환경 변수가 이김(운영과 동일).
     */
    public static String resolveApiKeyForTest() {
        String key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        key = System.getenv("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        Properties p = loadApplicationProperties();
        String raw = p.getProperty("ai.llm.api-key");
        return unwrapSpringDefault(raw != null ? raw : "");
    }

    public static String resolveLlmModelForTest(String fallbackIfUnset) {
        Properties p = loadApplicationProperties();
        String raw = p.getProperty("ai.llm.model");
        if (raw == null || raw.isBlank()) {
            return fallbackIfUnset;
        }
        String m = unwrapSpringDefault(raw);
        return m.isBlank() ? fallbackIfUnset : m;
    }

    public static int resolveLlmTimeoutSecondsForTest(int fallback) {
        Properties p = loadApplicationProperties();
        String raw = p.getProperty("ai.llm.timeout-seconds");
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(unwrapSpringDefault(raw).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static int resolveLlmMaxRetriesForTest(int fallback) {
        Properties p = loadApplicationProperties();
        String raw = p.getProperty("ai.llm.max-retries");
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(unwrapSpringDefault(raw).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
