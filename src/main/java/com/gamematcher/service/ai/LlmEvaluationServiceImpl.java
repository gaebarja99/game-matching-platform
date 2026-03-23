package com.gamematcher.service.ai;

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
            @Value("${ai.llm.model:gpt-4o-mini}") String model,
            @Value("${ai.llm.timeout-seconds:15}") int timeoutSeconds,
            @Value("${ai.llm.max-retries:2}") int maxRetries) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
    }

    @Override
    public Optional<LlmEvaluationResponseDTO> evaluate(String promptText) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("AI_API_KEY가 설정되지 않아 LLM 평가를 건너뜁니다.");
            return Optional.empty();
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String responseText = callOpenAi(promptText);
                String cleanJson = extractJsonFromResponse(responseText);
                LlmEvaluationResponseDTO dto = objectMapper.readValue(cleanJson, LlmEvaluationResponseDTO.class);
                return Optional.ofNullable(dto);
            } catch (Exception e) {
                log.warn("LLM 평가 API 호출 실패 (시도 {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private String callOpenAi(String promptText) throws Exception {
        var service = new com.theokanning.openai.service.OpenAiService(
                apiKey,
                Duration.ofSeconds(timeoutSeconds));

        var request = com.theokanning.openai.completion.chat.ChatCompletionRequest.builder()
                .model(model)
                .messages(java.util.List.of(
                        new com.theokanning.openai.completion.chat.ChatMessage("user", promptText)))
                .temperature(0.5)
                .build();

        var completion = service.createChatCompletion(request);
        var message = completion.getChoices().stream()
                .findFirst()
                .map(c -> c.getMessage().getContent())
                .orElse(null);

        if (message == null || message.isBlank()) {
            throw new IllegalStateException("LLM 응답 내용이 비어 있습니다.");
        }
        return message;
    }

    private String extractJsonFromResponse(String raw) {
        var matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return raw.trim();
    }
}
