package com.gamematcher.dto.ai.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 평가 API 응답 DTO.
 * 프롬프트에 요청한 JSON 형태(summary, detailedComment)를 파싱한다.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmEvaluationResponseDTO {

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("detailedComment")
    private String detailedComment;
}
