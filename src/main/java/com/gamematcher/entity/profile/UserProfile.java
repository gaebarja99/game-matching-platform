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

    @Column(name = "profile_updated_at")
    private LocalDateTime profileUpdatedAt;

    @PrePersist
    @PreUpdate
    protected void touchProfileUpdatedAt() {
        profileUpdatedAt = LocalDateTime.now();
    }
}
