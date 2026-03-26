package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Valorant 매치 메타데이터 (1 매치 = 1 row)
 * AI 분석용 상세 데이터
 */
@Entity
@Table(name = "valorant_match", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, length = 100)
    private String matchId;

    @Column(length = 100)
    private String map;

    @Column(name = "game_version", length = 50)
    private String gameVersion;

    @Column(name = "game_length")
    private Integer gameLength;

    @Column(name = "game_start")
    private Long gameStart;

    @Column(name = "game_start_patched", length = 50)
    private String gameStartPatched;

    @Column(name = "rounds_played")
    private Integer roundsPlayed;

    @Column(length = 50)
    private String mode;

    @Column(name = "mode_id", length = 50)
    private String modeId;

    @Column(length = 50)
    private String queue;

    @Column(name = "season_id", length = 50)
    private String seasonId;

    @Column(length = 50)
    private String platform;

    @Column(length = 50)
    private String region;

    @Column(length = 50)
    private String cluster;

    @Column(name = "red_rounds_won")
    private Integer redRoundsWon;

    @Column(name = "blue_rounds_won")
    private Integer blueRoundsWon;

    @Column(name = "red_has_won")
    private Boolean redHasWon;

    @Column(name = "blue_has_won")
    private Boolean blueHasWon;

    @Column(name = "premier_tournament_id", length = 100)
    private String premierTournamentId;

    @Column(name = "premier_matchup_id", length = 100)
    private String premierMatchupId;

    @Column(name = "api_cached_at")
    private LocalDateTime apiCachedAt;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorantMatchPlayer> players = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorantMatchRound> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorantKillEvent> killEvents = new ArrayList<>();
}
