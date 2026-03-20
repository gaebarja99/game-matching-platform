# Phase 4: LLM 통합 구현 가이드

데이터 가공이 완료된 시점에서 LLM API 연동 및 자연어 평가 서비스를 구현하는 방법입니다.

---

## 1. 구현 순서 요약

| 순서 | 작업 | 파일 |
|------|------|------|
| 1 | API 클라이언트 의존성 추가 | `pom.xml` |
| 2 | LLM 설정 속성 추가 | `application.properties` |
| 3 | LLM 응답 DTO 정의 | `LlmEvaluationResponseDTO` 등 |
| 4 | LlmEvaluationService 구현 | `LlmEvaluationService.java` |
| 5 | AIEvaluationService 통합 (Phase 5와 연계) | `AIEvaluationService.java` |

---

## 2. 의존성 선택 및 추가

### 옵션 A: OpenAI API (가장 보편적)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.theokanning.openai-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>
```

- Maven Central 등록, Spring Boot와 호환
- `OpenAiService`로 간단히 호출
- 모델: `gpt-4o-mini`, `gpt-4o`, `gpt-4-turbo` 등

### 옵션 B: OpenAI 공식 SDK (최신)

```xml
<dependency>
    <groupId>com.openai</groupId>
    <artifactId>openai-java</artifactId>
    <version>0.27.0</version>
</dependency>
```

### 옵션 C: Anthropic Claude

```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-sdk-java</artifactId>
</dependency>
```

**권장**: `openai-java` 또는 `com.theokanning.openai-java` (사용량 많음, 문서 많음)

---

## 3. 환경 설정

```properties
# application.properties 추가

# LLM API (OpenAI 예시)
ai.llm.provider=openai
ai.llm.api-key=${AI_API_KEY:}
ai.llm.model=gpt-4o-mini
ai.llm.timeout-seconds=30
ai.llm.max-retries=2

# OpenAI Base URL (프록시/대체 엔드포인트용)
# ai.llm.openai.base-url=https://api.openai.com
```

- `AI_API_KEY`: 환경 변수로 관리 (운영 권장)
- 로컬 테스트 시 `application-local.properties` 등에 키 설정

---

## 4. DTO 및 인터페이스

### 4.1 LLM 응답 DTO

```java
// com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO
package com.gamematcher.dto.ai.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmEvaluationResponseDTO {

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("detailedComment")
    private String detailedComment;
}
```

### 4.2 LLM 서비스 인터페이스 (선택)

```java
// com.gamematcher.service.ai.LlmEvaluationService
public interface LlmEvaluationService {
    /**
     * @param promptText LLM에 전달할 프롬프트 전문
     * @return summary, detailedComment. 실패 시 null
     */
    Optional<LlmEvaluationResponseDTO> evaluate(String promptText);
}
```

---

## 5. LlmEvaluationService 구현

### 5.1 프롬프트 구성 규칙

반드시 포함할 내용:

1. **역할**: "발로란트 전문 전술 분석가 및 코치"
2. **입력**: 가공된 매치 스탯(JSON 또는 요약 텍스트)
3. **출력 형식 명시**:
   ```
   반드시 아래 JSON 형태로만 응답하세요. 다른 설명 없이 JSON만 출력하세요.
   {"summary":"한 줄 요약 (50자 이내)","detailedComment":"상세 코멘트 (200자 이내)"}
   ```
4. **언어**: "모든 응답은 반드시 한국어로 작성하세요."

### 5.2 프롬프트 예시

```
당신은 발로란트 전문 전술 분석가 및 코치입니다.
아래 플레이어의 매치 스탯을 분석하여 요약과 상세 코멘트를 작성해주세요.

[매치 스탯]
- 매치 결과: 승리
- 점수: 125점 (등급 A)
- KDA: 18/12/4
- KAST: 72%
- ADR: 142
- 헤드샷율: 28%
- First Blood: 3, First Death: 2
- 라운드 승률: 55%

반드시 아래 JSON 형태로만 응답하세요. 다른 설명 없이 JSON만 출력하세요.
{"summary":"한 줄 요약 (50자 이내)","detailedComment":"상세 코멘트 (200자 이내)"}
모든 응답은 반드시 한국어로 작성하세요.
```

### 5.3 구현 스켈레톤 (OpenAI openai-java 기준)

```java
@Service
public class LlmEvaluationServiceImpl implements LlmEvaluationService {

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final int maxRetries;

    public LlmEvaluationServiceImpl(
            ObjectMapper objectMapper,
            @Value("${ai.llm.api-key:}") String apiKey,
            @Value("${ai.llm.model:gpt-4o-mini}") String model,
            @Value("${ai.llm.timeout-seconds:30}") int timeoutSeconds,
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
            return Optional.empty(); // Fallback
        }

        try {
            // 1. API 호출 (retry, timeout 적용)
            String responseText = callApiWithRetry(promptText);

            // 2. JSON 파싱 (마크다운 코드블록 제거 후)
            String cleanJson = extractJsonFromResponse(responseText);
            LlmEvaluationResponseDTO dto = objectMapper.readValue(cleanJson, LlmEvaluationResponseDTO.class);

            return Optional.ofNullable(dto);
        } catch (Exception e) {
            // 로깅 후 Fallback
            return Optional.empty();
        }
    }

    private String extractJsonFromResponse(String raw) {
        // ```json ... ``` 또는 ``` ... ``` 블록 제거
        var m = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```").matcher(raw);
        if (m.find()) return m.group(1).trim();
        return raw.trim();
    }

    private String callApiWithRetry(String promptText) throws Exception {
        // OpenAI API 호출 구현
        // Retry, Timeout 적용
        throw new UnsupportedOperationException("OpenAI SDK로 구현");
    }
}
```

---

## 6. OpenAI 호출 구현 예시 (openai-java 사용)

```java
// 의존성: com.theokanning.openai-java:service
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
ChatCompletionRequest req = ChatCompletionRequest.builder()
    .model(model)
    .messages(List.of(new ChatMessage("user", promptText)))
    .temperature(0.3)
    .build();

String content = service.createChatCompletion(req)
    .getChoices().get(0).getMessage().getContent();
```

---

## 7. Fallback 동작

| 상황 | 동작 |
|------|------|
| API 키 없음 | `summary`, `detailedComment` = null 또는 "AI 분석을 사용할 수 없습니다." |
| API 호출 실패 | 규칙 기반 점수·등급만 반환 |
| JSON 파싱 실패 | `summary`, `detailedComment` = null |

---

## 8. Rate Limit / Retry / Timeout

- **Timeout**: `ai.llm.timeout-seconds` (기본 30초)
- **Retry**: `maxRetries` 만큼 429 등에 대해 재시도 (exponential backoff 권장)
- **Rate Limit**: 사용량 한도 설정 (일일 호출 수 등)은 별도 레이어에서 처리

---

## 9. AIEvaluationService 연동 흐름

```
1. RuleBasedScoreService.calculateScore() / calculateGrade() 호출
2. 점수·등급 획득
3. PromptBuilder로 프롬프트 생성 (스탯 → 텍스트)
4. LlmEvaluationService.evaluate(promptText)
5. 성공 시 → summary, detailedComment 매핑
6. 실패 시 → summary, detailedComment = null
7. MatchRecordEvaluation 저장
```

---

## 10. 다음 단계 체크리스트

- [ ] `pom.xml`에 OpenAI/Anthropic 의존성 추가
- [ ] `application.properties`에 `ai.llm.*` 설정 추가
- [ ] `LlmEvaluationResponseDTO` 생성
- [ ] `LlmEvaluationService` + 구현체 작성
- [ ] 프롬프트 빌더 (스탯 → 텍스트 변환) 구현
- [ ] `AIEvaluationService`에서 규칙 기반 + LLM 결과 결합
- [ ] 단위 테스트: API 키 없을 때 Fallback, Mock으로 성공 시나리오
