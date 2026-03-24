package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Valorant MMR History API 응답 데이터 (매치별 MMR 변동 기록)
 * puuid는 API URL에서 조회 시 알 수 있음
 */
@Entity
@Table(name = "valorant_mmr_history_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"puuid", "match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMmrHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String puuid;

    @Column(name = "match_id", nullable = false, length = 100)
    private String matchId;

    @Column(name = "current_tier")
    private Integer currentTier;

    @Column(name = "current_tier_patched", length = 50)
    private String currentTierPatched;

    @Column(name = "map_id", length = 100)
    private String mapId;

    @Column(name = "map_name", length = 100)
    private String mapName;

    @Column(name = "season_id", length = 100)
    private String seasonId;

    @Column(name = "ranking_in_tier")
    private Integer rankingInTier;

    @Column(name = "mmr_change_to_last_game")
    private Integer mmrChangeToLastGame;

    private Integer elo;
    private String date;

    @Column(name = "date_raw")
    private Long dateRaw;

    @Column(name = "image_small", length = 255)
    private String imageSmall;

    @Column(name = "image_large", length = 255)
    private String imageLarge;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
