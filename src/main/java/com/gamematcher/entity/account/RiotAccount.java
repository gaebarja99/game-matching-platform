package com.gamematcher.entity.account;

import com.gamematcher.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Riot 계정 (LoL / TFT / Valorant 공통)
 */
@Entity
@Table(name = "riot_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"puuid"})
})
@Getter
@Setter
@NoArgsConstructor
public class RiotAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 100)
    private String puuid;

    @Column(name = "game_name", nullable = false, length = 100)
    private String gameName;

    @Column(name = "tag_line", nullable = false, length = 20)
    private String tagLine;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
