package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recruit_posts")
@Getter
@Setter
@NoArgsConstructor
public class RecruitPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 게임 구분: LEAGUE_OF_LEGENDS, TFT, VALORANT, PUBG, STEAM, PALWORLD, OVERWATCH, OTHERS */
    @Column(name = "game", nullable = false, length = 32)
    private String game;

    @Column(name = "summoner_name", nullable = false, length = 100)
    private String summonerName;

    @Column(name = "main_position", length = 50)
    private String mainPosition;

    @Column(name = "find_position", length = 50)
    private String findPosition;

    @Column(name = "tier", length = 32)
    private String tier;

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "mode", length = 32)
    private String mode;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
