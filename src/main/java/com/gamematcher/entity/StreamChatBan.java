package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 방송별 채팅 금지 사용자. expiresAt 이 있으면 해당 시각까지만 금지(타이머).
 */
@Entity
@Table(name = "stream_chat_bans", uniqueConstraints = @UniqueConstraint(columnNames = {"stream_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StreamChatBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stream_id", nullable = false)
    private Long streamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 만료 시각. null이면 영구 금지. 지나면 자동 해제. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
