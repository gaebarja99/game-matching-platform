package com.gamematcher.entity.match.lol;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * LoL 매치 참가자별 AI 평가 (규칙 기반 점수 + LLM 요약/상세).
 */
@Entity
@Table(name = "lol_match_ai_evaluation", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_lol_ai_eval_participant_model",
                columnNames = {"lol_match_participant_id", "llm_model"}
        )
})
@Getter
@Setter
@NoArgsConstructor
public class LolMatchAiEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lol_match_participant_id", nullable = false)
    private LolMatchParticipant lolMatchParticipant;

    @Column(name = "llm_model", nullable = false, length = 128)
    private String llmModel = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.PENDING;

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

    public LolMatchAiEvaluation(LolMatchParticipant participant) {
        this.lolMatchParticipant = participant;
        this.llmModel = "";
        this.status = EvaluationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public LolMatchAiEvaluation(
            LolMatchParticipant participant,
            String llmModel,
            EvaluationStatus status,
            Integer score,
            String summary,
            String detailedComment
    ) {
        this.lolMatchParticipant = participant;
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
