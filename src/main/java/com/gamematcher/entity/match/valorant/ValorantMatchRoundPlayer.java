package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Valorant 라운드별 플레이어 통계 (라운드×플레이어 단위)
 * AI 분석용 라운드별 개인 성과 분석
 */
@Entity
@Table(name = "valorant_match_round_player", indexes = {
        @Index(columnList = "round_id, player_puuid")
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatchRoundPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private ValorantMatchRound round;

    @Column(name = "player_puuid", nullable = false, length = 100)
    private String playerPuuid;

    @Column(name = "player_display_name", length = 100)
    private String playerDisplayName;

    @Column(name = "player_team", length = 20)
    private String playerTeam;

    private Integer damage;
    private Integer headshots;
    private Integer bodyshots;
    private Integer legshots;
    private Integer kills;
    private Integer score;

    @Column(name = "ability_x_casts")
    private Integer abilityXCasts;

    @Column(name = "ability_e_casts")
    private Integer abilityECasts;

    @Column(name = "ability_q_casts")
    private Integer abilityQCasts;

    @Column(name = "ability_c_casts")
    private Integer abilityCCasts;

    @Column(name = "loadout_value")
    private Integer loadoutValue;

    @Column(name = "economy_remaining")
    private Integer economyRemaining;

    @Column(name = "economy_spent")
    private Integer economySpent;

    @Column(name = "was_afk")
    private Boolean wasAfk;

    @Column(name = "was_penalized")
    private Boolean wasPenalized;

    @Column(name = "stayed_in_spawn")
    private Boolean stayedInSpawn;

    @OneToMany(mappedBy = "roundPlayer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorantRoundPlayerDamageEvent> damageEvents = new ArrayList<>();
}
