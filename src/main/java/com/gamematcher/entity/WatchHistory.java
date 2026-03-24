package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 시청자별·방송별 시청 시간 누적 (같은 방송을 여러 번 시청해도 합산) */
@Entity
@Table(name = "watch_histories", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "viewer_id", "stream_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viewer_id", nullable = false)
    private Long viewerId;

    @Column(name = "stream_id", nullable = false)
    private Long streamId;

    /** 누적 시청 시간 (초) */
    @Column(name = "watch_seconds", nullable = false)
    private Long watchSeconds = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
