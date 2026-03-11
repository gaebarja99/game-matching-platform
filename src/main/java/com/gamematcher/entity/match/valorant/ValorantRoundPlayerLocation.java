package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant 라운드 내 플레이어 위치 (plant/defuse 시점)
 * player_locations_on_plant, player_locations_on_defuse
 */
@Entity
@Table(name = "valorant_round_player_location", indexes = {
        @Index(columnList = "round_id, event_type")
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantRoundPlayerLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private ValorantMatchRound round;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType; // "PLANT" or "DEFUSE"

    @Column(name = "player_puuid", nullable = false, length = 100)
    private String playerPuuid;

    @Column(name = "player_display_name", length = 100)
    private String playerDisplayName;

    @Column(name = "player_team", length = 20)
    private String playerTeam;

    @Column(name = "location_x")
    private Integer locationX;

    @Column(name = "location_y")
    private Integer locationY;

    @Column(name = "view_radians")
    private Double viewRadians;
}
