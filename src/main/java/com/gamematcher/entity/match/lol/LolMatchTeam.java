package com.gamematcher.entity.match.lol;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LoL 매치 팀 정보 (1 매치 = 2 row)
 */
@Entity
@Table(name = "lol_match_team", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "team_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class LolMatchTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private LolMatch match;

    @Column(name = "team_id", nullable = false)
    private Integer teamId;

    @Column(nullable = false)
    private boolean win;

    /** bans: [{championId, pickTurn}, ...] JSON 문자열 */
    @Column(name = "bans", columnDefinition = "json")
    private String bans;

    /** objectives: { "champion": {first, kills}, "tower": {...}, ... } JSON 문자열 */
    @Column(name = "objectives", columnDefinition = "json")
    private String objectives;
}
