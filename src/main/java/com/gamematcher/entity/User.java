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
     * API ?ㅻ뜑 {@code X-Auth-Token} 議고쉶??蹂묓빀??GM2 ?먮쫫).
     * 濡쒓렇?맞룰??끒텽Auth ?깃났 ??諛쒓툒; nullable? 留덉씠洹몃젅?댁뀡 吏곹썑 湲곗〈 ?됱슜.
     */
    @Column(name = "auth_token", length = 128, unique = true)
    private String authToken;

    /** OAuth ?쒓났??(GOOGLE ??. null?대㈃ ?쇰컲 ?꾩씠??鍮꾨?踰덊샇 媛??*/
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Provider provider;

    /** OAuth ?쒓났??履??ъ슜??怨좎쑀 ID (?? Google sub) */
    @Column(name = "provider_subject", length = 128)
    private String providerSubject;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    /** ?먭린?뚭컻 (?꾨줈???몄쭛) */
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

    /** ???붿븸 (1??= 1.2?? */
    @Column(name = "pang_balance", nullable = false)
    private Long pangBalance = 0L;

    /** 寃쏀뿕移?(0.1 ?⑥쐞 ??? 1 XP = 10, 0.1 XP = 1). ??1 援щℓ=1 XP, ?붾줈???꾩썝 諛?梨꾪똿 1媛?0.1 XP */
    @Column(name = "total_experience_tenths", nullable = false)
    private Long totalExperienceTenths = 0L;

    /** 留덉씪由ъ?(??. ??援щℓ ??寃곗젣 湲덉븸??10% ?곷┰ (1??1.2????0.12?? */
    @Column(name = "mileage", nullable = false)
    private Long mileage = 0L;

    /** 愿묎퀬 ?쒓굅 留뚮즺?쇱떆 (null ?먮뒗 ?꾩옱 ?댁쟾?대㈃ 愿묎퀬 ?몄텧) */
        @Column(name = "ad_free_until")
    private LocalDateTime adFreeUntil;

    @Column(name = "profanity_strike_count", nullable = false)
    private Integer profanityStrikeCount = 0;

    @Column(name = "chat_muted_until")
    private LocalDateTime chatMutedUntil;

    /** ?ㅽ듃由щ㉧ 援щ텇: ?쇰컲(30% ?섏닔猷? / ?뚰듃??20% ?섏닔猷?. null?대㈃ ?쇰컲怨??숈씪 泥섎━ */
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

