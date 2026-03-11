package com.gamematcher.entity.community;

import com.gamematcher.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_post_bookmarks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "user_id"})
}, indexes = {
        @Index(name = "idx_bookmark_post", columnList = "post_id"),
        @Index(name = "idx_bookmark_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PostBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
