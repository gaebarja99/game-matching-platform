package com.gamematcher.dto.valorant;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 발로란트 AI 평가 결과 DTO.
 * 규칙 기반 점수 + LLM 응답(summary, detailedComment)을 포함한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValorantAiEvaluationResponseDto {

    /** 매치 ID */
    private String matchId;

    /** 플레이어 PUUID */
    private String playerPuuid;

    /** 플레이어 표시명 (name#tag) */
    private String playerDisplayName;

    /** 에이전트 */
    private String agent;

    /** 팀 (Red/Blue) */
    private String team;

    /** 평가 상태 */
    private EvaluationStatus status;

    /** 규칙 기반 점수 (0~200) */
    private Integer score;

    /** 등급 (S/A/B/C/D) */
    private Grade grade;

    /** AI 요약 (500자 내외) */
    private String summary;

    /** AI 상세 코멘트 */
    private String detailedComment;

    /** 평가 시각 */
    private LocalDateTime evaluatedAt;
}
