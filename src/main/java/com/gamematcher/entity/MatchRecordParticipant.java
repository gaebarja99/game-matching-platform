package com.gamematcher.entity;

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

    @Column(name = "raw_stats", columnDefinition = "clob")
    private String rawStats;

    public MatchRecordParticipant(MatchRecord matchRecord, User user, String role, String rawStats) {
        this.matchRecord = matchRecord;
        this.user = user;
        this.role = role;
        this.rawStats = rawStats;
    }
}
