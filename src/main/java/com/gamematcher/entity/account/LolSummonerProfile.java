package com.gamematcher.entity.account;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * LoL 소환사 프로필 (Riot Summoner-v4 by-puuid API 응답 저장)
 * LolSummonerProfileDto와 매핑
 */
@Entity
@Table(name = "lol_summoner_profile", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"puuid"})
})
@Getter
@Setter
@NoArgsConstructor
public class LolSummonerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String puuid;

    @Column(name = "profile_icon_id")
    private Integer profileIconId;

    @Column(name = "revision_date")
    private Long revisionDate;

    @Column(name = "summoner_level")
    private Integer summonerLevel;

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
