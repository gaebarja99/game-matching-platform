package com.gamematcher.dto.lol;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LoL 매치 AI 평가 API 응답 (전적 화면·발로 응답과 동일 계열).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LolAiEvaluationResponseDto {

    private String matchId;
    private String playerPuuid;
    private String playerDisplayName;
    private String champion;
    private String teamPosition;
    private EvaluationStatus status;
    private Integer score;
    private Grade grade;
    private String summary;
    private String detailedComment;
    private String llmModel;
    private LocalDateTime evaluatedAt;
}
