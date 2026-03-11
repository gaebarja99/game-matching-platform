package com.gamematcher.entity.match.lol;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * LoL 매치 타임라인 (Riot Match-v5 Timeline)
 * frames 전체를 JSON으로 저장
 */
@Entity
@Table(name = "lol_match_timeline", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class LolMatchTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private LolMatch match;

    @Column(name = "frame_interval")
    private Integer frameInterval;

    @Column(name = "end_of_game_result", length = 50)
    private String endOfGameResult;

    /** info 전체 (frames, participants 등) - JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "timeline_info", columnDefinition = "json")
    private Object timelineInfo;
}
