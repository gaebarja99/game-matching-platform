package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "match_records")
@Getter
@Setter
@NoArgsConstructor
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "match_id", nullable = false, length = 255)
    private String matchId;

    @Column(length = 50)
    private String result;

    @Column(name = "played_at")
    private LocalDateTime playedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "clob")
    private Map<String, Object> rawData;

    public MatchRecord(Game game, String matchId, String result, LocalDateTime playedAt, Map<String, Object> rawData) {
        this.game = game;
        this.matchId = matchId;
        this.result = result;
        this.playedAt = playedAt;
        this.rawData = rawData;
    }
}
