package com.gamematcher.entity.account;

import com.gamematcher.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Discord 계정
 */
@Entity
@Table(name = "discord_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"discord_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class DiscordAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "discord_id", nullable = false, unique = true, length = 50)
    private String discordId;

    @Column(length = 100)
    private String username;

    @Column(length = 500)
    private String avatar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
