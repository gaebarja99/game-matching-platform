package com.gamematcher.dto.lol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * LoL 매치 참가자 DTO (Riot Match-v5 API info.participants[])
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LolParticipantDto {

    @JsonProperty("puuid")
    private String puuid;

    @JsonProperty("summonerId")
    private String summonerId;

    @JsonProperty("riotIdGameName")
    private String riotIdGameName;

    @JsonProperty("riotIdTagline")
    private String riotIdTagline;

    @JsonProperty("championId")
    private Integer championId;

    @JsonProperty("championName")
    private String championName;

    @JsonProperty("teamId")
    private Integer teamId;

    @JsonProperty("individualPosition")
    private String individualPosition;

    @JsonProperty("teamPosition")
    private String teamPosition;

    @JsonProperty("kills")
    private Integer kills;

    @JsonProperty("deaths")
    private Integer deaths;

    @JsonProperty("assists")
    private Integer assists;

    @JsonProperty("win")
    private Boolean win;

    @JsonProperty("goldEarned")
    private Integer goldEarned;

    @JsonProperty("goldSpent")
    private Integer goldSpent;

    @JsonProperty("totalDamageDealtToChampions")
    private Integer totalDamageDealtToChampions;

    @JsonProperty("totalDamageTaken")
    private Integer totalDamageTaken;

    @JsonProperty("visionScore")
    private Integer visionScore;

    @JsonProperty("totalMinionsKilled")
    private Integer totalMinionsKilled;

    @JsonProperty("neutralMinionsKilled")
    private Integer neutralMinionsKilled;

    @JsonProperty("champLevel")
    private Integer champLevel;

    @JsonProperty("item0")
    private Integer item0;

    @JsonProperty("item1")
    private Integer item1;

    @JsonProperty("item2")
    private Integer item2;

    @JsonProperty("item3")
    private Integer item3;

    @JsonProperty("item4")
    private Integer item4;

    @JsonProperty("item5")
    private Integer item5;

    @JsonProperty("item6")
    private Integer item6;

    @JsonProperty("summoner1Id")
    private Integer summoner1Id;

    @JsonProperty("summoner2Id")
    private Integer summoner2Id;

    @JsonProperty("participantId")
    private Integer participantId;

    @JsonProperty("gameEndedInEarlySurrender")
    private Boolean gameEndedInEarlySurrender;

    @JsonProperty("gameEndedInSurrender")
    private Boolean gameEndedInSurrender;

    @JsonProperty("challenges")
    private Map<String, Object> challenges;

    @JsonProperty("perks")
    private Perks perks;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Perks {
        @JsonProperty("statPerks")
        private StatPerks statPerks;

        private List<Style> styles;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class StatPerks {
            private Integer defense;
            private Integer flex;
            private Integer offense;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Style {
            private String description;
            private Integer style;
            private List<Selection> selections;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Selection {
            private Integer perk;
            private Integer var1;
            private Integer var2;
            private Integer var3;
        }
    }
}
