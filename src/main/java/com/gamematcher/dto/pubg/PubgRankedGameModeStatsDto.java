package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 랭크 게임 모드별 스탯 (squad, squad-fpp, duo 등)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRankedGameModeStatsDto {

    private PubgTierDto currentTier;
    private Integer currentRankPoint;
    private PubgTierDto bestTier;
    private Integer bestRankPoint;

    private Integer roundsPlayed;
    private Double avgRank;
    private Double avgSurvivalTime;
    private Double top10Ratio;
    private Double winRatio;

    private Integer assists;
    private Integer wins;
    private Double kda;
    private Double kdr;
    private Double avgKill;
    private Integer kills;
    private Integer deaths;
    private Integer roundMostKills;
    private Double longestKill;

    private Integer headshotKills;
    private Double headshotKillRatio;
    private Double damageDealt;

    @JsonProperty("dBNOs")
    private Integer dBNOs;

    private Double reviveRatio;
    private Integer revives;
    private Integer heals;
    private Integer boosts;
    private Integer weaponsAcquired;
    private Integer teamKills;
    private Integer playTime;
    private Integer killStreak;
}
