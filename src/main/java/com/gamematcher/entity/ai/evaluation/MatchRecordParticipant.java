package com.gamematcher.entity.ai.evaluation;

import com.gamematcher.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match_record_participants")
@Getter
@Setter
@NoArgsConstructor
public class MatchRecordParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_record_id", nullable = false)
    private MatchRecord matchRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)
    private String role;

    @Lob
    @Column(name = "raw_stats")
    private String rawStats;

    public MatchRecordParticipant(MatchRecord matchRecord, User user, String role, String rawStats) {
        this.matchRecord = matchRecord;
        this.user = user;
        this.role = role;
        this.rawStats = rawStats;
    }
}
