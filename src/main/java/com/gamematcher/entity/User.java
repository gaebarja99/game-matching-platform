package com.gamematcher.entity;

import com.gamematcher.constant.Provider;
import com.gamematcher.constant.Role;
import com.gamematcher.constant.StreamerTier;
import com.gamematcher.constant.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true, length = 128)
    private String loginId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(length = 50)
    private String nickname;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    /**
     * API 헤더 {@code X-Auth-Token} 조회용(병합된 GM2 흐름).
     * 로그인·가입·OAuth 성공 시 발급; nullable은 마이그레이션 직후 기존 행용.
     */
    @Column(name = "auth_token", length = 128, unique = true)
    private String authToken;

    /** OAuth 제공자 (GOOGLE 등). null이면 일반 아이디/비밀번호 가입 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Provider provider;

    /** OAuth 제공자 쪽 사용자 고유 ID (예: Google sub) */
    @Column(name = "provider_subject", length = 128)
    private String providerSubject;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    /** 자기소개 (프로필 편집) */
    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** 팡 잔액 (1팡 = 1.2원) */
    @Column(name = "pang_balance", nullable = false)
    private Long pangBalance = 0L;

    /** 경험치 (0.1 단위 저장: 1 XP = 10, 0.1 XP = 1). 팡 1 구매=1 XP, 팔로우/후원 방 채팅 1개=0.1 XP */
    @Column(name = "total_experience_tenths", nullable = false)
    private Long totalExperienceTenths = 0L;

    /** 마일리지(원). 팡 구매 시 결제 금액의 10% 적립 (1팡=1.2원 → 0.12원) */
    @Column(name = "mileage", nullable = false)
    private Long mileage = 0L;

    /** 광고 제거 만료일시 (null 또는 현재 이전이면 광고 노출) */
    @Column(name = "ad_free_until")
    private LocalDateTime adFreeUntil;

    /** 스트리머 구분: 일반(30% 수수료) / 파트너(20% 수수료). null이면 일반과 동일 처리 */
    @Enumerated(EnumType.STRING)
    @Column(name = "streamer_tier", length = 20)
    private StreamerTier streamerTier;

    /** 욕설 필터 누적 횟수 (채팅 검열) */
    @Column(name = "profanity_strike_count")
    private Integer profanityStrikeCount;

    /** 채팅 금지 해제 시각 (null 또는 과거이면 허용) */
    @Column(name = "chat_muted_until")
    private LocalDateTime chatMutedUntil;

    /** 정지 해제 예정 시각 (영구 정지 등은 null) */
    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

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
