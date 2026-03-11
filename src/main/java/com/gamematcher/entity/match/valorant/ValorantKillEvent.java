package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Valorant 킬 이벤트 (매치 내 모든 킬)
 * AI 분석용 킬 패턴, 무기별 통계 등
 */
@Entity
@Table(name = "valorant_kill_event", indexes = {
        @Index(columnList = "match_id"),
        @Index(columnList = "killer_puuid"),
        @Index(columnList = "victim_puuid"),
        @Index(columnList = "damage_weapon_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantKillEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private ValorantMatch match;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "kill_time_in_round")
    private Integer killTimeInRound;

    @Column(name = "kill_time_in_match")
    private Integer killTimeInMatch;

    @Column(name = "killer_puuid", length = 100)
    private String killerPuuid;

    @Column(name = "killer_display_name", length = 100)
    private String killerDisplayName;

    @Column(name = "killer_team", length = 20)
    private String killerTeam;

    @Column(name = "victim_puuid", length = 100)
    private String victimPuuid;

    @Column(name = "victim_display_name", length = 100)
    private String victimDisplayName;

    @Column(name = "victim_team", length = 20)
    private String victimTeam;

    @Column(name = "victim_death_x")
    private Integer victimDeathX;

    @Column(name = "victim_death_y")
    private Integer victimDeathY;

    @Column(name = "damage_weapon_id", length = 100)
    private String damageWeaponId;

    @Column(name = "damage_weapon_name", length = 100)
    private String damageWeaponName;

    @Column(name = "secondary_fire_mode")
    private Boolean secondaryFireMode;

    @OneToMany(mappedBy = "killEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorantKillAssistant> assistants = new ArrayList<>();
}
