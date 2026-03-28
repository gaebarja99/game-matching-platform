package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림 받는 사용자(팔로워 등) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 알림 유형 (예: FOLLOWING_STARTED_STREAM) */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /** 관련 방송 ID */
    @Column(name = "stream_id")
    private Long streamId;

    /** 관련 사용자 ID (방송 시작한 스트리머 등) */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    /** 알림 메시지 */
    @Column(name = "message", length = 255)
    private String message;

    /** 읽음 여부 */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
