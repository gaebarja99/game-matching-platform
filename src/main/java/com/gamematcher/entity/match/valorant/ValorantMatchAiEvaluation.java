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
        @UniqueConstraint(columnNames = {"valorant_match_player_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatchAiEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valorant_match_player_id", nullable = false, unique = true)
    private ValorantMatchPlayer valorantMatchPlayer;

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
        this.status = EvaluationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public ValorantMatchAiEvaluation(ValorantMatchPlayer valorantMatchPlayer, EvaluationStatus status,
                                     Integer score, String summary, String detailedComment) {
        this.valorantMatchPlayer = valorantMatchPlayer;
        this.status = status;
        this.score = score;
        this.grade = Grade.fromScore(score);
        this.summary = summary;
        this.detailedComment = detailedComment;
        this.createdAt = LocalDateTime.now();
        this.evaluatedAt = LocalDateTime.now();
    }
}
