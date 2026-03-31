package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Valorant MMR 스냅샷 (Henrik API /valorant/v2/by-puuid/mmr/{region}/{puuid} 응답 저장)
 */
@Entity
@Table(name = "valorant_mmr_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"puuid"})
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMmrRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String puuid;

    @Column(length = 20)
    private String name;

    @Column(length = 10)
    private String tag;

    // --- current_data ---
    @Column(name = "current_tier")
    private Integer currentTier;

    @Column(name = "current_tier_patched", length = 50)
    private String currentTierPatched;

    @Column(name = "ranking_in_tier")
    private Integer rankingInTier;

    @Column(name = "mmr_change_to_last_game")
    private Integer mmrChangeToLastGame;

    private Integer elo;

    @Column(name = "games_needed_for_rating")
    private Integer gamesNeededForRating;

    @Column(name = "image_small", length = 500)
    private String imageSmall;

    @Column(name = "image_large", length = 500)
    private String imageLarge;

    // --- highest_rank ---
    @Column(name = "highest_tier")
    private Integer highestTier;

    @Column(name = "highest_tier_patched", length = 50)
    private String highestTierPatched;

    @Column(name = "highest_season", length = 20)
    private String highestSeason;

    /** by_season JSON (동적 키 구조, 대용량 허용) */
    @Lob
    @Column(name = "by_season_json", columnDefinition = "LONGTEXT")
    private String bySeasonJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
