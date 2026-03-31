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
     * API 인증용 토큰이다. 세션 로그인과 별개로 사용할 수 있다.
     */
    @Column(name = "auth_token", length = 128, unique = true)
    private String authToken;

    /**
     * OAuth 제공자 정보. 일반 회원가입 사용자는 null 일 수 있다.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Provider provider;

    /**
     * OAuth 제공자의 고유 사용자 ID. 예: Google sub
     */
    @Column(name = "provider_subject", length = 128)
    private String providerSubject;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    /**
     * 자기소개 문구
     */
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

    /**
     * 보유 중인 팡 잔액
     */
    @Column(name = "pang_balance", nullable = false)
    private Long pangBalance = 0L;

    /**
     * 총 경험치. 0.1 XP 단위로 저장한다.
     */
    @Column(name = "total_experience_tenths", nullable = false)
    private Long totalExperienceTenths = 0L;

    /**
     * 마일리지. 결제 금액의 일부를 적립한다.
     */
    @Column(name = "mileage", nullable = false)
    private Long mileage = 0L;

    /**
     * 광고 제거 만료 시각. null 이면 광고 제거 혜택이 없다.
     */
    @Column(name = "ad_free_until")
    private LocalDateTime adFreeUntil;

    @Column(name = "profanity_strike_count", nullable = false)
    private Integer profanityStrikeCount = 0;

    @Column(name = "chat_muted_until")
    private LocalDateTime chatMutedUntil;

    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "suspension_reason", length = 255)
    private String suspensionReason;

    /**
     * 스트리머 등급 정보. 일반 사용자는 null 일 수 있다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "streamer_tier", length = 20)
    private StreamerTier streamerTier;

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
