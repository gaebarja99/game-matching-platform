package com.gamematcher.entity.match.cs2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Counter-Strike 2 전적 (Steam Web API 기반)
 */
@Entity
@Table(name = "cs2_matches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"steam_id", "match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Cs2Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_id", nullable = false, length = 30)
    private String steamId;

    @Column(name = "match_id", nullable = false, length = 50)
    private String matchId;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false)
    private int assists;

    @Column(nullable = false)
    private int mvps;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean win;

    @Column(length = 50)
    private String map;

    @Column(name = "headshot_kills")
    private Integer headshotKills;

    @Column(name = "played_at")
    private Long playedAt; // Unix timestamp
}
