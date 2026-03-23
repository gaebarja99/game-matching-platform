package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant 킬 직후 생존 플레이어 위치 (player_locations_on_kill)
 * 킬 이벤트 시점에 아직 생존한 플레이어들의 위치·시선 정보.
 */
@Entity
@Table(name = "valorant_kill_event_player_location", indexes = {
        @Index(columnList = "kill_event_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantKillEventPlayerLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kill_event_id", nullable = false)
    private ValorantKillEvent killEvent;

    @Column(name = "player_puuid", nullable = false, length = 36)
    private String playerPuuid;

    /** Riot ID 표시명: GameName#TagLine 최대 22자 */
    @Column(name = "player_display_name", length = 50)
    private String playerDisplayName;

    @Column(name = "player_team", length = 100)
    private String playerTeam;

    @Column(name = "location_x")
    private Integer locationX;

    @Column(name = "location_y")
    private Integer locationY;

    @Column(name = "view_radians")
    private Double viewRadians;
}
