package com.gamematcher.entity;

import com.gamematcher.constant.GameList;
import com.gamematcher.constant.StreamStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_streams")
@Getter
@Setter
@NoArgsConstructor
public class LiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stream_key", unique = true, length = 64)
    private String streamKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameList game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StreamStatus status = StreamStatus.CREATED;

    @Column(name = "playback_url", length = 500)
    private String playbackUrl;

    @Column(name = "external_url", length = 500)
    private String externalUrl;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "chat_frozen", nullable = false)
    private Boolean chatFrozen = false;

    @Column(name = "stream_notice", length = 200)
    private String streamNotice;

    @Column(name = "stream_notice_visible", nullable = false)
    private Boolean streamNoticeVisible = false;

    @Column(name = "min_video_pang")
    private Integer minVideoPang;

    @Column(name = "min_tts_pang")
    private Integer minTtsPang;

    @Column(name = "chat_scope", nullable = false, length = 20)
    private String chatScope = "ALL";

    @Column(name = "chat_permission_scope", nullable = false, length = 20)
    private String chatPermissionScope = "ALL";

    @Column(name = "slow_mode_enabled", nullable = false)
    private Boolean slowModeEnabled = false;

    @Column(name = "slow_mode_seconds", nullable = false)
    private Integer slowModeSeconds = 5;

    @Column(name = "chat_rules", length = 2000)
    private String chatRules;

    @Column(name = "visible_in_recent", nullable = false)
    private Boolean visibleInRecent = true;

    @Column(name = "warning_count", nullable = false)
    private Integer warningCount = 0;

    @Column(name = "last_warning_at")
    private LocalDateTime lastWarningAt;

    @Column(name = "last_warning_message", length = 200)
    private String lastWarningMessage;

    @Column(name = "admin_warning_count", nullable = false)
    private Integer adminWarningCount = 0;

    @Column(name = "last_admin_warning_at")
    private LocalDateTime lastAdminWarningAt;

    @Column(name = "last_admin_warning_message", length = 200)
    private String lastAdminWarningMessage;

    @Column(name = "last_admin_id")
    private Long lastAdminId;

    @PrePersist
    protected void onCreate() {
        if (chatScope == null || chatScope.isBlank()) {
            chatScope = "ALL";
        }
        if (chatPermissionScope == null || chatPermissionScope.isBlank()) {
            chatPermissionScope = chatScope;
        }
        if (slowModeEnabled == null) {
            slowModeEnabled = false;
        }
        if (slowModeSeconds == null) {
            slowModeSeconds = 5;
        }
        if (visibleInRecent == null) {
            visibleInRecent = true;
        }
        if (warningCount == null) {
            warningCount = 0;
        }
        if (adminWarningCount == null) {
            adminWarningCount = 0;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (chatScope == null || chatScope.isBlank()) {
            chatScope = chatPermissionScope != null && !chatPermissionScope.isBlank() ? chatPermissionScope : "ALL";
        }
        if (chatPermissionScope == null || chatPermissionScope.isBlank()) {
            chatPermissionScope = chatScope != null && !chatScope.isBlank() ? chatScope : "ALL";
        }
        if (warningCount == null) {
            warningCount = 0;
        }
        if (adminWarningCount == null) {
            adminWarningCount = 0;
        }
        updatedAt = LocalDateTime.now();
    }
}
