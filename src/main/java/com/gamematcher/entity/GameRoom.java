package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 게임 방 (제목, 메모, 삭제 비밀번호, 게임별 옵션, 방장·마감) */
@Entity
@Table(name = "game_rooms")
@Getter
@Setter
@NoArgsConstructor
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "memo", length = 1000)
    private String memo;

    @Column(name = "delete_password", nullable = false, length = 64)
    private String deletePassword;

    @Column(name = "game", nullable = false, length = 32)
    private String game;

    /** 게임별 옵션 (JSON 또는 key=value 형태, 예: tier=GOLD, position=TOP) */
    @Column(name = "game_options", length = 500)
    private String gameOptions;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(name = "group_chat_room_id")
    private Long groupChatRoomId;

    @Column(name = "closed", nullable = false)
    private boolean closed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
