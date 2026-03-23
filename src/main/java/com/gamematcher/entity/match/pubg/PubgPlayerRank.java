package com.gamematcher.entity.match.pubg;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 플레이어 시즌별 랭크 스탯 (게임 모드별 1 row)
 * API: GET /shards/{platform}/players/{accountId}/seasons/{seasonId}/ranked
 */
@Entity
@Table(name = "pubg_player_rank", indexes = {
        @Index(columnList = "player_id, season_id, platform, game_mode", unique = true),
        @Index(columnList = "player_id"),
        @Index(columnList = "season_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PubgPlayerRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PUBG account ID (account.xxx) */
    @Column(name = "player_id", nullable = false, length = 80)
    private String playerId;

    /** 시즌 ID (division.bro.official.pc-2018-40) */
    @Column(name = "season_id", nullable = false, length = 100)
    private String seasonId;

    /** 플랫폼 (steam, psn, xbox, kakao) */
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    /** 게임 모드 (squad, squad-fpp, duo, duo-fpp, solo, solo-fpp) */
    @Column(name = "game_mode", nullable = false, length = 20)
    private String gameMode;

    @Column(name = "current_tier", length = 30)
    private String currentTier;

    @Column(name = "sub_tier", length = 10)
    private String subTier;

    @Column(name = "current_rank_point")
    private Integer currentRankPoint;

    @Column(name = "best_tier", length = 30)
    private String bestTier;

    @Column(name = "best_sub_tier", length = 10)
    private String bestSubTier;

    @Column(name = "best_rank_point")
    private Integer bestRankPoint;

    @Column(name = "rounds_played")
    private Integer roundsPlayed;

    @Column(name = "avg_rank")
    private Double avgRank;

    @Column(name = "avg_survival_time")
    private Double avgSurvivalTime;

    @Column(name = "top10_ratio")
    private Double top10Ratio;

    @Column(name = "win_ratio")
    private Double winRatio;

    @Column(name = "wins")
    private Integer wins;

    @Column(name = "kills")
    private Integer kills;

    @Column(name = "deaths")
    private Integer deaths;

    @Column(name = "assists")
    private Integer assists;

    @Column(name = "avg_kill")
    private Double avgKill;

    @Column(name = "round_most_kills")
    private Integer roundMostKills;

    @Column(name = "longest_kill")
    private Double longestKill;

    @Column(name = "headshot_kills")
    private Integer headshotKills;

    @Column(name = "headshot_kill_ratio")
    private Double headshotKillRatio;

    @Column(name = "damage_dealt")
    private Double damageDealt;

    @Column(name = "dbnos")
    private Integer dbnos;

    @Column(name = "revive_ratio")
    private Double reviveRatio;

    @Column(name = "revives")
    private Integer revives;

    @Column(name = "heals")
    private Integer heals;

    @Column(name = "boosts")
    private Integer boosts;

    @Column(name = "weapons_acquired")
    private Integer weaponsAcquired;

    @Column(name = "team_kills")
    private Integer teamKills;

    @Column(name = "play_time")
    private Integer playTime;

    @Column(name = "kill_streak")
    private Integer killStreak;
}
