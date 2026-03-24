package com.gamematcher.entity.match.pubg;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PUBG 시즌 마스터 데이터
 * API 시즌 목록을 DB에 캐시하여 rate limit 절약 및 빠른 조회
 */
@Entity
@Table(name = "pubg_season", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform", "season_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PubgSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 플랫폼: steam, psn, xbox, kakao, stadia
     */
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    /**
     * API season id (예: division.bro.official.pc-2018-40)
     */
    @Column(name = "season_id", nullable = false, length = 100)
    private String seasonId;

    @Column(name = "is_current_season")
    private Boolean isCurrentSeason;

    @Column(name = "is_offseason")
    private Boolean isOffseason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
