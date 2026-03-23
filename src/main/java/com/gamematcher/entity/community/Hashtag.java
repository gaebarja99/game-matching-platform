package com.gamematcher.entity.community;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "community_hashtags", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name")
}, indexes = {
        @Index(name = "idx_hashtag_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
