package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Steam 보유 게임 (선택 저장)
 */
@Entity
@Table(name = "steam_owned_games", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"steam_id", "app_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class SteamOwnedGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_id", nullable = false, length = 50)
    private String steamId;

    @Column(name = "app_id", nullable = false, length = 20)
    private String appId;

    @Column(name = "game_name", length = 255)
    private String gameName;

    @Column(name = "playtime_minutes")
    private Integer playtimeMinutes;
}
