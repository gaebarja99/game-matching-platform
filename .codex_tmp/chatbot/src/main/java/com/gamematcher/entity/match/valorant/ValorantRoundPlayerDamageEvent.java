package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant 라운드 플레이어별 데미지 이벤트 (누구에게 얼마나 데미지)
 * RoundPlayerStat.damage_events
 */
@Entity
@Table(name = "valorant_round_player_damage_event", indexes = {
        @Index(columnList = "round_player_id"),
        @Index(columnList = "receiver_puuid")
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantRoundPlayerDamageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_player_id", nullable = false)
    private ValorantMatchRoundPlayer roundPlayer;

    @Column(name = "receiver_puuid", length = 36)
    private String receiverPuuid;

    /** Riot ID 표시명: GameName#TagLine 최대 22자 (한글·일본어 등 Unicode) */
    @Column(name = "receiver_display_name", length = 50)
    private String receiverDisplayName;

    @Column(name = "receiver_team", length = 100)
    private String receiverTeam;

    private Integer bodyshots;
    private Integer headshots;
    private Integer legshots;
    private Integer damage;
}
