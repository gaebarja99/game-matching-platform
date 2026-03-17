package com.gamematcher.entity.match.pubg;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 매치 참가자별 스탯 (1 매치 = N row)
 * PubgParticipantIncludedDto, PubgParticipantStatsDto 에 대응
 */
@Entity
@Table(name = "pubg_match_participant", indexes = {
        @Index(columnList = "match_id, participant_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class PubgMatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private PubgMatch match;

    /** API included participant 엔티티 ID (UUID) */
    @Column(name = "participant_id", nullable = false, length = 64)
    private String participantId;

    /** PUBG account ID (account.xxx) */
    @Column(name = "player_id", nullable = false, length = 80)
    private String playerId;

    @Column(name = "name", length = 50)
    private String name;

    private Integer dbnos;
    private Integer assists;
    private Integer boosts;

    @Column(name = "damage_dealt")
    private Double damageDealt;

    @Column(name = "death_type", length = 20)
    private String deathType;

    @Column(name = "headshot_kills")
    private Integer headshotKills;

    private Integer heals;

    @Column(name = "kill_place")
    private Integer killPlace;

    @Column(name = "kill_streaks")
    private Integer killStreaks;

    private Integer kills;

    @Column(name = "longest_kill")
    private Double longestKill;

    private Integer revives;

    @Column(name = "ride_distance")
    private Double rideDistance;

    @Column(name = "road_kills")
    private Integer roadKills;

    @Column(name = "swim_distance")
    private Double swimDistance;

    @Column(name = "team_kills")
    private Integer teamKills;

    @Column(name = "time_survived")
    private Integer timeSurvived;

    @Column(name = "vehicle_destroys")
    private Integer vehicleDestroys;

    @Column(name = "walk_distance")
    private Double walkDistance;

    @Column(name = "weapons_acquired")
    private Integer weaponsAcquired;

    @Column(name = "win_place")
    private Integer winPlace;

    @Column(nullable = false)
    private boolean win;
}
