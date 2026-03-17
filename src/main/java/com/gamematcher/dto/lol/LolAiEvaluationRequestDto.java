package com.gamematcher.dto.lol;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LoL 플레이어 AI 평가 API 요청 DTO.
 */
@Data
@NoArgsConstructor
public class LolAiEvaluationRequestDto {

    /** 매치 JSON (필수, 단일 또는 matches/data 래퍼 형태) */
    @NotNull(message = "matchJson은 필수입니다.")
    private String matchJson;

    /** 타임라인 JSON (선택, 없으면 타임라인 이벤트 없이 분석) */
    private String timelineJson;

    /** 평가할 플레이어의 participants 인덱스 (0-based) */
    @NotNull(message = "playerIndex는 필수입니다.")
    @Min(0)
    @Max(9)
    private Integer playerIndex;
}
