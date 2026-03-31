package com.gamematcher.entity.match.apex;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Apex Legends 전적 (Mozambique Here API 기반)
 */
@Entity
@Table(name = "apex_matches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uid", "match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ApexMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Apex UID */
    @Column(nullable = false, length = 30)
    private String uid;

    @Column(name = "match_id", nullable = false, length = 100)
    private String matchId;

    @Column(length = 50)
    private String legend;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false)
    private int assists;

    @Column(nullable = false)
    private int damage;

    @Column(nullable = false)
    private boolean win;

    @Column(length = 50)
    private String map;

    @Column(name = "placement")
    private Integer placement; // 순위 (1 = 우승)

    @Column(name = "played_at")
    private Long playedAt; // Unix timestamp
}
