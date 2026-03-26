package com.gamematcher.entity.match.lol;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LoL 매치 상세 (Riot Match-v5 API)
 * 1 매치 = 1 row
 */
@Entity
@Table(name = "lol_match", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class LolMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, length = 50)
    private String matchId;

    @Column(name = "data_version", length = 10)
    private String dataVersion;

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "game_creation")
    private Long gameCreation;

    @Column(name = "game_duration")
    private Long gameDuration;

    @Column(name = "game_start_timestamp")
    private Long gameStartTimestamp;

    @Column(name = "game_end_timestamp")
    private Long gameEndTimestamp;

    @Column(name = "game_mode", length = 50)
    private String gameMode;

    @Column(name = "game_type", length = 50)
    private String gameType;

    @Column(name = "game_name", length = 100)
    private String gameName;

    @Column(name = "game_version", length = 50)
    private String gameVersion;

    @Column(name = "map_id")
    private Integer mapId;

    @Column(name = "queue_id")
    private Integer queueId;

    @Column(name = "platform_id", length = 20)
    private String platformId;

    @Column(name = "end_of_game_result", length = 50)
    private String endOfGameResult;

    @Column(name = "tournament_code", length = 50)
    private String tournamentCode;

    /** 전적 검색 캐시: 마지막 API 반영 시각 (null 이면 즉시 갱신 대상) */
    @Column(name = "api_cached_at")
    private LocalDateTime apiCachedAt;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LolMatchParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LolMatchTeam> teams = new ArrayList<>();

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private LolMatchTimeline timeline;
}
