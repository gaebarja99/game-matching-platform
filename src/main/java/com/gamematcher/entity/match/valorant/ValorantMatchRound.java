package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Valorant 매치 라운드별 이벤트 (1 매치 = N round)
 * AI 분석용 라운드 단위 패턴 분석
 */
@Entity
@Table(name = "valorant_match_round", indexes = {
        @Index(columnList = "match_id, round_index")
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatchRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private ValorantMatch match;

    @Column(name = "round_index", nullable = false)
    private Integer roundIndex;

    @Column(name = "winning_team", length = 100)
    private String winningTeam;

    @Column(name = "end_type", length = 50)
    private String endType;

    @Column(name = "bomb_planted")
    private Boolean bombPlanted;

    @Column(name = "bomb_defused")
    private Boolean bombDefused;

    @Column(name = "plant_site", length = 20)
    private String plantSite;

    @Column(name = "plant_time_in_round")
    private Integer plantTimeInRound;

    @Column(name = "planted_by_puuid", length = 36)
    private String plantedByPuuid;

    @Column(name = "plant_location_x")
    private Integer plantLocationX;

    @Column(name = "plant_location_y")
    private Integer plantLocationY;

    @Column(name = "defuse_time_in_round")
    private Integer defuseTimeInRound;

    @Column(name = "defused_by_puuid", length = 36)
    private String defusedByPuuid;

    @Column(name = "defuse_location_x")
    private Integer defuseLocationX;

    @Column(name = "defuse_location_y")
    private Integer defuseLocationY;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorantMatchRoundPlayer> playerStats = new ArrayList<>();

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorantRoundPlayerLocation> playerLocations = new ArrayList<>();
}
