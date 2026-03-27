package com.gamematcher.entity.match.pubg;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PUBG 매치 메타데이터 (1 매치 = 1 row)
 * PubgMatchApiResponse.data, PubgMatchAttributesDto 에 대응
 */
@Entity
@Table(name = "pubg_match", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PubgMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, length = 100)
    private String matchId;

    @Column(name = "game_mode", length = 50)
    private String gameMode;

    @Column(name = "title_id", length = 50)
    private String titleId;

    @Column(name = "shard_id", length = 20)
    private String shardId;

    @Column(name = "map_name", length = 50)
    private String mapName;

    @Column(name = "is_custom_match")
    private Boolean isCustomMatch;

    @Column(name = "season_state", length = 20)
    private String seasonState;

    @Column(name = "created_at", length = 50)
    private String createdAt;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "match_type", length = 20)
    private String matchType;

    @Column(name = "api_cached_at")
    private LocalDateTime apiCachedAt;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PubgMatchParticipant> participants = new ArrayList<>();
}
