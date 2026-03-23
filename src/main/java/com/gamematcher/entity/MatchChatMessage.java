package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 랜덤 매칭 채팅 메시지 */
@Entity
@Table(name = "match_chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class MatchChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
