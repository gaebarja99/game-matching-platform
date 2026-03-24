package com.gamematcher.entity.community;

import com.gamematcher.entity.User;
import com.gamematcher.constant.community.RecommendType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_post_recommends", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "user_id"})
}, indexes = {
        @Index(name = "idx_recommend_post", columnList = "post_id"),
        @Index(name = "idx_recommend_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PostRecommend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendType recommendType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
