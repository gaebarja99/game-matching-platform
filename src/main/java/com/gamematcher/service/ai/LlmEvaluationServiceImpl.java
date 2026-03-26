package com.gamematcher.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * OpenAI API를 이용해 프롬프트를 LLM에 전달하고 JSON 형태의 평가 응답을 파싱한다.
 */
@Service
public class LlmEvaluationServiceImpl implements LlmEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(LlmEvaluationServiceImpl.class);
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final int maxRetries;

    public LlmEvaluationServiceImpl(
            ObjectMapper objectMapper,
            @Value("${ai.llm.api-key:}") String apiKey,
            @Value("${ai.llm.model:gpt-5-mini}") String model,
            @Value("${ai.llm.timeout-seconds:30}") int timeoutSeconds,
            @Value("${ai.llm.max-retries:2}") int maxRetries) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
    }

    @Override
    public Optional<LlmEvaluationResponseDTO> evaluate(String promptText, String modelOverride) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("AI_API_KEY가 설정되지 않아 LLM 평가를 건너뜁니다.");
            return Optional.empty();
        }

        String useModel = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride.trim() : model;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String responseText;
            try {
                responseText = callOpenAi(promptText, useModel);
            } catch (Exception e) {
                log.warn("OpenAI Chat Completions 호출 실패 (시도 {}/{}, model={}): {}",
                        attempt, maxRetries, useModel, e.getMessage(), e);
                if (attempt == maxRetries) {
                    return Optional.empty();
                }
                continue;
            }
            try {
                String cleanJson = extractJsonFromResponse(responseText);
                LlmEvaluationResponseDTO dto = parseEvaluationDto(cleanJson);
                if (dto != null && hasText(dto.getSummary(), dto.getDetailedComment())) {
                    return Optional.of(dto);
                }
                String trimmedClean = cleanJson.trim();
                if (dto != null && !looksLikeJsonObject(trimmedClean)) {
                    dto = new LlmEvaluationResponseDTO();
                    dto.setSummary(null);
                    dto.setDetailedComment(responseText.trim());
                    return Optional.of(dto);
                }
                String head = responseText.length() > 400 ? responseText.substring(0, 400) + "…" : responseText;
                log.warn("LLM 응답에 요약/상세 텍스트가 없음 (시도 {}/{}). 앞부분: {}",
                        attempt, maxRetries, head.replaceAll("\\s+", " "));
                if (attempt == maxRetries) {
                    return Optional.empty();
                }
            } catch (Exception e) {
                String head = responseText.length() > 400 ? responseText.substring(0, 400) + "…" : responseText;
                log.warn("LLM 응답 JSON 파싱 실패 (시도 {}/{}): {} | 응답 앞부분: {}",
                        attempt, maxRetries, e.getMessage(), head.replaceAll("\\s+", " "), e);
                if (attempt == maxRetries) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private String callOpenAi(String promptText, String useModel) throws Exception {
        var service = new com.theokanning.openai.service.OpenAiService(
                apiKey,
                Duration.ofSeconds(timeoutSeconds));

        String m = (useModel != null && !useModel.isBlank()) ? useModel.trim() : model;
        // temperature 등 샘플링 파라미터는 모델·엔드포인트마다 제한이 달라 보내지 않고 API 기본값 사용
        var request = com.theokanning.openai.completion.chat.ChatCompletionRequest.builder()
                .model(m)
                .messages(java.util.List.of(
                        new com.theokanning.openai.completion.chat.ChatMessage("user", promptText)))
                .build();

        var completion = service.createChatCompletion(request);
        var message = completion.getChoices().stream()
                .findFirst()
                .map(c -> c.getMessage().getContent())
                .orElse(null);

        if (message == null || message.isBlank()) {
            throw new IllegalStateException(
                    "assistant.message.content가 비어 있습니다. 최신 모델은 JSON 응답 구조가 달라 theokanning 라이브러리(0.18.x)가 content를 채우지 못하는 경우가 있습니다.");
        }
        return message;
    }

    private String extractJsonFromResponse(String raw) {
        var matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String t = raw.trim();
        int start = t.indexOf('{');
        if (start >= 0) {
            String balanced = extractBalancedJsonObject(t, start);
            if (balanced != null) {
                return balanced;
            }
        }
        return t;
    }

    /** 첫 번째 '{'부터 중괄호 균형이 맞는 구간만 잘라 JSON으로 쓴다. */
    private static String extractBalancedJsonObject(String s, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private static boolean looksLikeJsonObject(String t) {
        return t.startsWith("{") && t.endsWith("}");
    }

    private static boolean hasText(String summary, String detailed) {
        return (summary != null && !summary.isBlank()) || (detailed != null && !detailed.isBlank());
    }

    /**
     * DTO 매핑 + 루트에 없으면 흔한 대체 키로 문자열 추출.
     */
    private LlmEvaluationResponseDTO parseEvaluationDto(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        LlmEvaluationResponseDTO dto = objectMapper.treeToValue(root, LlmEvaluationResponseDTO.class);
        if (dto == null) {
            dto = new LlmEvaluationResponseDTO();
        }
        if (!hasText(dto.getSummary(), null)) {
            dto.setSummary(firstNonBlankText(root,
                    "summary", "요약", "SUMMARY", "short_summary", "shortSummary"));
        }
        if (!hasText(null, dto.getDetailedComment())) {
            dto.setDetailedComment(firstNonBlankText(root,
                    "detailedComment", "detailed_comment", "comment", "body", "analysis",
                    "detailed", "long_comment", "longComment", "상세", "상세코멘트"));
        }
        return dto;
    }

    private static String firstNonBlankText(JsonNode root, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode n = root.get(name);
            if (n != null && n.isTextual()) {
                String s = n.asText().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return null;
    }
}
