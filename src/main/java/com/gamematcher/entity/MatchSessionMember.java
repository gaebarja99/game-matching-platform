package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 랜덤 매칭 세션 참가자 */
@Entity
@Table(name = "match_session_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "session_id", "user_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class MatchSessionMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** LoL 5인 매칭 시 배정 라인 (TOP, JUNGLE, …). 기존 2인 매칭 등에서는 null */
    @Column(name = "assigned_lane", length = 24)
    private String assignedLane;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
