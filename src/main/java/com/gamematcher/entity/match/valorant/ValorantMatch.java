package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant 전적 (HenrikDev API 기반)
 */
@Entity
@Table(name = "valorant_matches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"puuid", "match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(name = "match_id", nullable = false, length = 100)
    private String matchId;

    @Column(length = 50)
    private String agent;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false)
    private int assists;

    @Column(nullable = false)
    private boolean win;

    @Column(length = 100)
    private String map;

    @Column(name = "rounds_won")
    private Integer roundsWon;

    @Column(name = "rounds_lost")
    private Integer roundsLost;
}
