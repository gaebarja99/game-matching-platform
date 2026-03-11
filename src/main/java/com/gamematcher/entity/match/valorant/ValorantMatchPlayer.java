package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant 매치별 플레이어 상세 통계 (1 매치 = 10 row)
 * AI 분석용 플레이어별 세부 지표
 */
@Entity
@Table(name = "valorant_match_player", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "puuid"})
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private ValorantMatch match;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String tag;

    @Column(length = 20)
    private String team;

    @Column(length = 50)
    private String character;

    private Integer level;

    @Column(name = "current_tier")
    private Integer currentTier;

    @Column(name = "current_tier_patched", length = 20)
    private String currentTierPatched;

    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Integer score;

    @Column(name = "headshots")
    private Integer headshots;

    @Column(name = "bodyshots")
    private Integer bodyshots;

    @Column(name = "legshots")
    private Integer legshots;

    @Column(name = "damage_made")
    private Integer damageMade;

    @Column(name = "damage_received")
    private Integer damageReceived;

    @Column(name = "ability_x_cast")
    private Integer abilityXCast;

    @Column(name = "ability_e_cast")
    private Integer abilityECast;

    @Column(name = "ability_q_cast")
    private Integer abilityQCast;

    @Column(name = "ability_c_cast")
    private Integer abilityCCast;

    @Column(name = "economy_spent_overall")
    private Integer economySpentOverall;

    @Column(name = "loadout_value_overall")
    private Integer loadoutValueOverall;

    @Column(name = "afk_rounds")
    private Integer afkRounds;

    @Column(name = "rounds_in_spawn")
    private Integer roundsInSpawn;

    @Column(name = "friendly_fire_incoming")
    private Integer friendlyFireIncoming;

    @Column(name = "friendly_fire_outgoing")
    private Integer friendlyFireOutgoing;

    @Column(nullable = false)
    private boolean win;
}
