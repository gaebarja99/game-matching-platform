package com.gamematcher.entity.match.lol;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LoL 매치 참가자 (1 매치 = 10 row)
 */
@Entity
@Table(name = "lol_match_participant", indexes = {
        @Index(columnList = "match_id, participant_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class LolMatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private LolMatch match;

    @Column(name = "participant_id", nullable = false)
    private Integer participantId;

    @Column(nullable = false, length = 36)
    private String puuid;

    @Column(name = "summoner_id", length = 100)
    private String summonerId;

    /** Riot Game Name: 3~16자 (한글·일본어 등 Unicode 지원) */
    @Column(name = "riot_id_game_name", length = 20)
    private String riotIdGameName;

    /** Riot Tag Line: 3~5자 (한글·일본어 등 Unicode 지원) */
    @Column(name = "riot_id_tagline", length = 10)
    private String riotIdTagline;

    @Column(name = "champion_id")
    private Integer championId;

    @Column(name = "champion_name", length = 50)
    private String championName;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "individual_position", length = 20)
    private String individualPosition;

    @Column(name = "team_position", length = 20)
    private String teamPosition;

    private Integer kills;
    private Integer deaths;
    private Integer assists;

    @Column(nullable = false)
    private boolean win;

    @Column(name = "gold_earned")
    private Integer goldEarned;

    @Column(name = "gold_spent")
    private Integer goldSpent;

    @Column(name = "total_damage_dealt_to_champions")
    private Integer totalDamageDealtToChampions;

    @Column(name = "total_damage_taken")
    private Integer totalDamageTaken;

    @Column(name = "vision_score")
    private Integer visionScore;

    @Column(name = "total_minions_killed")
    private Integer totalMinionsKilled;

    @Column(name = "neutral_minions_killed")
    private Integer neutralMinionsKilled;

    @Column(name = "champ_level")
    private Integer champLevel;

    @Column(name = "item0")
    private Integer item0;

    @Column(name = "item1")
    private Integer item1;

    @Column(name = "item2")
    private Integer item2;

    @Column(name = "item3")
    private Integer item3;

    @Column(name = "item4")
    private Integer item4;

    @Column(name = "item5")
    private Integer item5;

    @Column(name = "item6")
    private Integer item6;

    @Column(name = "summoner1_id")
    private Integer summoner1Id;

    @Column(name = "summoner2_id")
    private Integer summoner2Id;
}
