package com.gamematcher.entity.match.valorant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant 킬 어시스트 (1 킬당 0~N명)
 */
@Entity
@Table(name = "valorant_kill_assistant", indexes = {
        @Index(columnList = "kill_event_id"),
        @Index(columnList = "assistant_puuid")
})
@Getter
@Setter
@NoArgsConstructor
public class ValorantKillAssistant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kill_event_id", nullable = false)
    private ValorantKillEvent killEvent;

    @Column(name = "assistant_puuid", nullable = false, length = 36)
    private String assistantPuuid;

    /** Riot ID 표시명: GameName#TagLine 최대 22자 (한글·일본어 등 Unicode) */
    @Column(name = "assistant_display_name", length = 50)
    private String assistantDisplayName;

    @Column(name = "assistant_team", length = 100)
    private String assistantTeam;
}
