package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_profile_settings")
@Getter
@Setter
@NoArgsConstructor
public class ChannelProfileSettings {

    @Id
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "social_links_json", length = 4000)
    private String socialLinksJson;

    @Column(name = "cafe_enabled", nullable = false)
    private Boolean cafeEnabled = false;

    @Column(name = "sponsor_ranking_visible", nullable = false)
    private Boolean sponsorRankingVisible = false;

    @Column(name = "mission_visible", nullable = false)
    private Boolean missionVisible = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (cafeEnabled == null) cafeEnabled = false;
        if (sponsorRankingVisible == null) sponsorRankingVisible = false;
        if (missionVisible == null) missionVisible = false;
        updatedAt = LocalDateTime.now();
    }
}
