package com.gamematcher.entity.community;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "community_post_hashtags", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "hashtag_id"})
}, indexes = {
        @Index(name = "idx_post_hashtag_post", columnList = "post_id"),
        @Index(name = "idx_post_hashtag_hashtag", columnList = "hashtag_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;
}
