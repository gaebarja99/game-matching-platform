package com.gamematcher.entity.account;

import com.gamematcher.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Blizzard Battle.net 계정
 */
@Entity
@Table(name = "blizzard_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "region"})
})
@Getter
@Setter
@NoArgsConstructor
public class BlizzardAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "battle_tag", nullable = false, length = 50)
    private String battleTag;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(length = 20)
    private String region;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
