package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 친구 간 1:1 쪽지(채팅) 메시지 */
@Entity
@Table(name = "dm_messages", indexes = {
    @Index(name = "idx_dm_from_to_created", columnList = "from_user_id, to_user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class DmMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
