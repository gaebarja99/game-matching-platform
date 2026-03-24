package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "stream_id", nullable = false)
    private Long streamId;

    /** 방송 주인(스트리머) userId */
    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "video_url", length = 512)
    private String videoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
