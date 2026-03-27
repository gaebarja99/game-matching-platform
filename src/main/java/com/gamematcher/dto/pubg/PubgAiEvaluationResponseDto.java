package com.gamematcher.dto.pubg;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * PUBG AI 평가 API 응답 DTO (전적 화면·LLM 결과 연동).
 */
@Getter
@ToString
@Builder
public class PubgAiEvaluationResponseDto {
    private final String matchId;
    private final String accountId;
    private final String playerName;

    private final String summary;
    private final String detailedComment;

    /** LLM 프롬프트 입력에 포함된 핵심 라인(디버깅/검증용, 필요 시 클라이언트에서 사용). */
    private final List<String> debugTimelinePreview;

    /** 저장 시 사용한 LLM 모델 ID */
    private final String llmModel;

    private final String status;
    private final String grade;
    private final Integer score;
}

