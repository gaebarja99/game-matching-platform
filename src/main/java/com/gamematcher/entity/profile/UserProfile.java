package com.gamematcher.entity.profile;

import com.gamematcher.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(name = "banner_image_url", length = 512)
    private String bannerImageUrl;

    @Column(name = "preferred_games", length = 500)
    private String preferredGames;

    /** null이면 공개로 간주(기존 데이터 호환) */
    @Column(name = "public_bio_visible")
    private Boolean publicBioVisible;

    @Column(name = "public_banner_visible")
    private Boolean publicBannerVisible;

    @Column(name = "public_profile_image_visible")
    private Boolean publicProfileImageVisible;

    @Column(name = "public_preferred_games_visible")
    private Boolean publicPreferredGamesVisible;

    /** null이면 공개로 간주 — 외부 계정 연동을 공개 프로필에 표시할지 */
    @Column(name = "public_discord_link_visible")
    private Boolean publicDiscordLinkVisible;
    @Column(name = "public_steam_link_visible")
    private Boolean publicSteamLinkVisible;
    @Column(name = "public_blizzard_link_visible")
    private Boolean publicBlizzardLinkVisible;
    @Column(name = "public_riot_link_visible")
    private Boolean publicRiotLinkVisible;
    /** Riot 연동의 LoL 랭크 요약 공개. null이면 공개로 간주 */
    @Column(name = "public_riot_lol_rank_visible")
    private Boolean publicRiotLolRankVisible;
    /** Riot 연동의 발로란트 티어 요약 공개. null이면 공개로 간주 */
    @Column(name = "public_riot_valorant_rank_visible")
    private Boolean publicRiotValorantRankVisible;

    @Column(name = "profile_updated_at")
    private LocalDateTime profileUpdatedAt;

    @PrePersist
    @PreUpdate
    protected void touchProfileUpdatedAt() {
        profileUpdatedAt = LocalDateTime.now();
    }
}
