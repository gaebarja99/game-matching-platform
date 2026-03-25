package com.gamematcher.entity;

import com.gamematcher.constant.LolPosition;
import com.gamematcher.constant.LolTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * LoL 5인 랜덤 매칭 대기열.
 * 매칭 완료 시 행은 삭제되며, {@code matched}는 대기 중에만 false로 유지됩니다.
 */
@Entity
@Table(name = "lol_matching_queue", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lol_queue_user", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MatchingQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "game_name", nullable = false, length = 64)
    private String gameName = "LEAGUE_OF_LEGENDS";

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 24)
    private LolTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 24)
    private LolPosition position;

    @Column(name = "matched", nullable = false)
    private Boolean matched = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (matched == null) matched = false;
    }
}
