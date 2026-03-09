package com.gamematcher.entity;

import com.gamematcher.constant.EvaluationStatus;
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

    @Column(length = 10)
    private String grade;

    @Column(length = 500)
    private String summary;

    @Column(name = "detailed_comment", columnDefinition = "clob")
    private String detailedComment;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    public MatchRecordEvaluation(MatchRecordParticipant participant) {
        this.participant = participant;
        this.status = EvaluationStatus.PENDING;
    }

    public MatchRecordEvaluation(MatchRecordParticipant participant, EvaluationStatus status,
                                 Integer score, String grade, String summary, String detailedComment) {
        this.participant = participant;
        this.status = status;
        this.score = score;
        this.grade = grade;
        this.summary = summary;
        this.detailedComment = detailedComment;
        this.evaluatedAt = LocalDateTime.now();
    }
}
