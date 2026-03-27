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

    @Column(nullable = false, unique = true, length = 120)
    private String puuid;

    /** Riot Game Name: 3~16자 (한글·일본어 등 Unicode 지원) */
    @Column(name = "game_name", nullable = false, length = 20)
    private String gameName;

    /** Riot Tag Line: 3~5자 (한글·일본어 등 Unicode 지원) */
    @Column(name = "tag_line", nullable = false, length = 10)
    private String tagLine;

    @Column(name = "game_type", length = 20)
    private String gameType;

    @Column(name = "verification_method", length = 40)
    private String verificationMethod;

    @Column(name = "ownership_verified", nullable = false)
    private boolean ownershipVerified;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
