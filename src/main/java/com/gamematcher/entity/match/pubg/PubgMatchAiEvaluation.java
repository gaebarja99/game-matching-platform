package com.gamematcher.entity.match.pubg;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PUBG 매치(플레이어=participant)별 AI 평가 결과.
 *
 * <p>프롬프트 입력에 포함되는 요약/상세 코멘트를 LLM 결과에서 저장하고,
 * 규칙 기반 점수/등급은 별도 계산하여 함께 보관합니다.</p>
 */
@Entity
@Table(
        name = "pubg_match_ai_evaluation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pubg_match_participant_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class PubgMatchAiEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pubg_match_participant_id", nullable = false, unique = true)
    private PubgMatchParticipant pubgMatchParticipant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.PENDING;

    /** 규칙 기반 점수 */
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

    public PubgMatchAiEvaluation(PubgMatchParticipant pubgMatchParticipant) {
        this.pubgMatchParticipant = pubgMatchParticipant;
        this.status = EvaluationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public PubgMatchAiEvaluation(
            PubgMatchParticipant pubgMatchParticipant,
            EvaluationStatus status,
            Integer score,
            String summary,
            String detailedComment
    ) {
        this.pubgMatchParticipant = pubgMatchParticipant;
        this.status = status;
        this.score = score;
        this.grade = Grade.fromScore(score);
        this.summary = summary;
        this.detailedComment = detailedComment;
        this.createdAt = LocalDateTime.now();
        this.evaluatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (evaluatedAt == null && (summary != null || detailedComment != null)) {
            evaluatedAt = LocalDateTime.now();
        }
    }
}

