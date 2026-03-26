package com.gamematcher.entity.account;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Valorant 계정 프로필 (Henrik API /valorant/v1/account/{name}/{tag} 응답 저장)
 */
@Entity
@Table(name = "valorant_account", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"puuid"})
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String puuid;

    @Column(length = 20)
    private String region;

    @Column(name = "account_level")
    private Integer accountLevel;

    @Column(length = 20)
    private String name;

    @Column(length = 10)
    private String tag;

    @Column(name = "card_id", length = 100)
    private String cardId;

    @Column(name = "card_small", length = 255)
    private String cardSmall;

    @Column(name = "card_large", length = 255)
    private String cardLarge;

    @Column(name = "card_wide", length = 255)
    private String cardWide;

    @Column(name = "last_update", length = 50)
    private String lastUpdate;

    @Column(name = "last_update_raw")
    private Long lastUpdateRaw;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Henrik 계정 API를 마지막으로 받아 반영한 시각 (전적 검색 캐시 신선도) */
    @Column(name = "api_cached_at")
    private LocalDateTime apiCachedAt;

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
