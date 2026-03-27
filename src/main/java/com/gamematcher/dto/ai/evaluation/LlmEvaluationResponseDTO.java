package com.gamematcher.dto.ai.evaluation;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"요약", "SUMMARY", "short_summary", "shortSummary"})
    private String summary;

    @JsonProperty("detailedComment")
    @JsonAlias({
            "detailed_comment",
            "comment",
            "body",
            "analysis",
            "detailed",
            "long_comment",
            "longComment",
            "상세",
            "상세코멘트"
    })
    private String detailedComment;
}
