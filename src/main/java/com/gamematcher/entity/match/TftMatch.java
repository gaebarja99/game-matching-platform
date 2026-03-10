package com.gamematcher.entity.match;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TFT 전적
 */
@Entity
@Table(name = "tft_matches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"puuid", "match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class TftMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(name = "match_id", nullable = false, length = 50)
    private String matchId;

    @Column(nullable = false)
    private int placement;

    @Column(nullable = false)
    private int level;

    @Column(name = "total_players")
    private Integer totalPlayers;

    @Column(columnDefinition = "TEXT")
    private String traits;

    @Column(columnDefinition = "TEXT")
    private String units;

    @Column(name = "game_duration")
    private Integer gameDuration;

    @Column(name = "game_creation")
    private Long gameCreation;
}
