package com.gamematcher.entity;

import com.gamematcher.constant.GameList;
import com.gamematcher.constant.StreamStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * OBS 등으로 송출되는 라이브 스트림 정보.
 * 스트림 키로 RTMP 수신 서버와 연동되고, HLS 재생 URL로 사이트에서 시청 가능.
 */
@Entity
@Table(name = "live_streams")
@Getter
@Setter
@NoArgsConstructor
public class LiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** OBS에 입력할 고유 스트림 키 (간편 모드·외부 연동 시 null) */
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

    /** HLS 재생 URL (사이트 뷰어에서 사용). 간편 모드 시 null */
    @Column(name = "playback_url", length = 500)
    private String playbackUrl;

    /** 간편 모드: 트위치/유튜브 방송 URL. 있으면 이걸 임베드해서 재생 */
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

    /** 채팅 얼리기: true면 방장만 채팅 가능 */
    @Column(name = "chat_frozen", nullable = false)
    private Boolean chatFrozen = false;

    /** BJ 공지 (최대 200자). 채팅창 상단 노출용 */
    @Column(name = "stream_notice", length = 200)
    private String streamNotice;

    /** BJ 공지 노출 여부 */
    @Column(name = "stream_notice_visible", nullable = false)
    private Boolean streamNoticeVisible = false;

    /** 영상 후원 최소 팡 (이 금액 이상일 때만 영상 URL 첨부 가능, null/0이면 제한 없음) */
    @Column(name = "min_video_pang")
    private Integer minVideoPang;

    /** TTS(후원 메시지) 최소 팡 (이 금액 이상일 때만 메시지/TTS 가능, null/0이면 제한 없음) */
    @Column(name = "min_tts_pang")
    private Integer minTtsPang;

    /** 최근 방송 목록 노출 여부 (운영자 숨김 시 false) */
    @Column(name = "visible_in_recent", nullable = false)
    private Boolean visibleInRecent = true;

    /** 운영자 경고 누적 횟수 */
    @Column(name = "admin_warning_count")
    private Integer adminWarningCount;

    @Column(name = "last_admin_warning_at")
    private LocalDateTime lastAdminWarningAt;

    @Column(name = "last_admin_warning_message", length = 500)
    private String lastAdminWarningMessage;

    /** 마지막 방송 관리 액션을 수행한 관리자 사용자 ID */
    @Column(name = "last_admin_id")
    private Long lastAdminId;

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
