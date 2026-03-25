package com.gamematcher.entity.ai.evaluation;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_record_evaluations")
@Getter
@Setter
@NoArgsConstructor
public class MatchRecordEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private MatchRecordParticipant participant;

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
    @Column(name = "detailed_comment")
    private String detailedComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    public MatchRecordEvaluation(MatchRecordParticipant participant) {
        this.participant = participant;
        this.status = EvaluationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public MatchRecordEvaluation(MatchRecordParticipant participant, EvaluationStatus status,
                                 Integer score, String summary, String detailedComment) {
        this.participant = participant;
        this.status = status;
        this.score = score;
        this.grade = Grade.fromScore(score);
        this.summary = summary;
        this.detailedComment = detailedComment;
        this.createdAt = LocalDateTime.now();
        this.evaluatedAt = LocalDateTime.now();
    }
}
