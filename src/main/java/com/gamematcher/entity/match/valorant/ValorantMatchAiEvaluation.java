package com.gamematcher.entity.match.valorant;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 발로란트 매치 플레이어별 AI 평가 결과.
 * 규칙 기반 점수 + LLM 응답(summary, detailedComment) 저장.
 */
@Entity
@Table(name = "valorant_match_ai_evaluation", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_valorant_ai_eval_player_model",
                columnNames = {"valorant_match_player_id", "llm_model"}
        )
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatchAiEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 동일 플레이어에 대해 모델별로 별도 행 저장 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "valorant_match_player_id", nullable = false)
    private ValorantMatchPlayer valorantMatchPlayer;

    /** OpenAI 모델 ID (예: gpt-5-mini). 레거시 행은 빈 문자열 */
    @Column(name = "llm_model", nullable = false, length = 128)
    private String llmModel = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.PENDING;

    /** 규칙 기반 점수 (0~200) */
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Grade grade;

    @Column(length = 500)
    private String summary;

    @Lob
    @Column(name = "detailed_comment", columnDefinition = "TEXT")
    private String detailedComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public ValorantMatchAiEvaluation(ValorantMatchPlayer valorantMatchPlayer) {
        this.valorantMatchPlayer = valorantMatchPlayer;
        this.llmModel = "";
        this.status = EvaluationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public ValorantMatchAiEvaluation(ValorantMatchPlayer valorantMatchPlayer, String llmModel, EvaluationStatus status,
                                     Integer score, String summary, String detailedComment) {
        this.valorantMatchPlayer = valorantMatchPlayer;
        this.llmModel = llmModel != null ? llmModel : "";
        this.status = status;
        this.score = score;
        this.grade = Grade.fromScore(score);
        this.summary = summary;
        this.detailedComment = detailedComment;
        this.createdAt = LocalDateTime.now();
        this.evaluatedAt = LocalDateTime.now();
    }
}
