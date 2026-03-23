package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 참가자 스탯 (participant.attributes.stats)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgParticipantStatsDto {

    @JsonProperty("DBNOs")
    private Integer DBNOs;
    private Integer assists;
    private Integer boosts;
    private Double damageDealt;
    private String deathType;
    private Integer headshotKills;
    private Integer heals;
    private Integer killPlace;
    private Integer killStreaks;
    private Integer kills;
    private Double longestKill;
    private String name;
    private String playerId;
    private Integer revives;
    private Double rideDistance;
    private Integer roadKills;
    private Double swimDistance;
    private Integer teamKills;
    private Integer timeSurvived;
    private Integer vehicleDestroys;
    private Double walkDistance;
    private Integer weaponsAcquired;
    private Integer winPlace;
}
