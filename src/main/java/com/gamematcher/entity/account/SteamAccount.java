package com.gamematcher.entity.account;

import com.gamematcher.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Steam 계정
 */
@Entity
@Table(name = "steam_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"steam_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class SteamAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "steam_id", nullable = false, unique = true, length = 50)
    private String steamId;

    @Column(name = "persona_name", length = 100)
    private String personaName;

    @Column(length = 500)
    private String avatar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
