package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 단체 채팅방 메시지 */
@Entity
@Table(name = "group_chat_messages", indexes = {
    @Index(name = "idx_group_chat_room_created", columnList = "room_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class GroupChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

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
