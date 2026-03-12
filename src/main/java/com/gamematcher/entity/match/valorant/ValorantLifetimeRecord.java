package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Valorant Lifetime API 매치별 플레이어 기록 (1 매치 1 플레이어 = 1 row)
 * Lifetime API 응답 데이터 저장용
 */
@Entity
@Table(name = "valorant_lifetime_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "puuid"})
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantLifetimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- meta ---
    @Column(name = "riot_match_id", nullable = false, length = 100)
    private String riotMatchId;

    @Column(name = "match_id", nullable = false, length = 100)
    private String matchId;

    @Column(name = "map_id", length = 100)
    private String mapId;

    @Column(name = "map_name", length = 100)
    private String mapName;

    @Column(length = 100)
    private String version;

    @Column(length = 50)
    private String mode;

    @Column(name = "started_at", length = 50)
    private String startedAt;

    @Column(name = "season_id", length = 100)
    private String seasonId;

    @Column(name = "season_short", length = 20)
    private String seasonShort;

    @Column(length = 50)
    private String region;

    @Column(length = 100)
    private String cluster;

    // --- stats (플레이어) ---
    @Column(nullable = false, length = 36)
    private String puuid;

    /** Riot Game Name */
    @Column(length = 20)
    private String name;

    /** Riot Tag Line */
    @Column(length = 10)
    private String tag;

    @Column(length = 20)
    private String team;

    private Integer level;

    @Column(name = "character_id", length = 100)
    private String characterId;

    @Column(name = "character_name", length = 50)
    private String characterName;

    private Integer tier;
    private Integer score;
    private Integer kills;
    private Integer deaths;
    private Integer assists;

    @Column(name = "shots_head")
    private Integer shotsHead;

    @Column(name = "shots_body")
    private Integer shotsBody;

    @Column(name = "shots_leg")
    private Integer shotsLeg;

    @Column(name = "damage_made")
    private Integer damageMade;

    @Column(name = "damage_received")
    private Integer damageReceived;

    // --- teams ---
    @Column(name = "red_rounds")
    private Integer redRounds;

    @Column(name = "blue_rounds")
    private Integer blueRounds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
