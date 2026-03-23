package com.gamematcher.entity.match.lol;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LoL 매치 요약 (match_summary 테이블, Riot API 요약용)
 * 상세 매치 데이터는 {@link LolMatch} 사용
 */
@Entity(name = "MatchSummary")
@Table(name = "match_summary", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"puuid", "match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class LolMatchSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(name = "match_id", nullable = false, length = 50)
    private String matchId;

    @Column(name = "champion_name", length = 50)
    private String championName;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false)
    private int assists;

    @Column(nullable = false)
    private boolean win;

    @Column(name = "team_position", length = 20)
    private String teamPosition;

    @Column(name = "total_damage")
    private int totalDamage;

    @Column(name = "vision_score")
    private int visionScore;

    @Column(nullable = false)
    private int cs;

    @Column(name = "game_duration")
    private int gameDuration;

    @Column(name = "game_creation")
    private long gameCreation;
}
