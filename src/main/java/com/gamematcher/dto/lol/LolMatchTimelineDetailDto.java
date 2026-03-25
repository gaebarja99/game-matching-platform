package com.gamematcher.dto.lol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * LoL 매치 타임라인 API 응답 DTO (Riot Match-v5 Timeline)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LolMatchTimelineDetailDto {

    private Metadata metadata;

    private Info info;

    // --- metadata (Riot Match-v5 API) ---
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        @JsonProperty("dataVersion")
        private String dataVersion;

        @JsonProperty("matchId")
        private String matchId;

        private List<String> participants;
    }

    // --- info (Riot Match-v5 Timeline info) ---
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        @JsonProperty("endOfGameResult")
        private String endOfGameResult;

        @JsonProperty("frameInterval")
        private Integer frameInterval;

        @JsonProperty("gameId")
        private Long gameId;

        private List<Frame> frames;

        private List<ParticipantInfo> participants;

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Frame {
            private List<TimelineEvent> events;

            @JsonProperty("participantFrames")
            private Map<String, ParticipantFrame> participantFrames;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TimelineEvent {
            @JsonProperty("type")
            private String type;

            @JsonProperty("timestamp")
            private Long timestamp;

            @JsonProperty("realTimestamp")
            private Long realTimestamp;

            @JsonProperty("participantId")
            private Integer participantId;

            @JsonProperty("itemId")
            private Integer itemId;

            @JsonProperty("skillSlot")
            private Integer skillSlot;

            @JsonProperty("levelUpType")
            private String levelUpType;

            @JsonProperty("creatorId")
            private Integer creatorId;

            @JsonProperty("wardType")
            private String wardType;

            @JsonProperty("position")
            private ParticipantFrame.Position position;

            @JsonProperty("teamId")
            private Integer teamId;

            @JsonProperty("laneType")
            private String laneType;

            @JsonProperty("killerId")
            private Integer killerId;

            @JsonProperty("monsterType")
            private String monsterType;

            @JsonProperty("monsterSubType")
            private String monsterSubType;

            @JsonProperty("winningTeam")
            private Integer winningTeam;

            @JsonProperty("level")
            private Integer level;

            @JsonProperty("victimId")
            private Integer victimId;

            @JsonProperty("assistingParticipantIds")
            private List<Integer> assistingParticipantIds;

            @JsonProperty("towerType")
            private String towerType;

            @JsonProperty("buildingType")
            private String buildingType;

            @JsonProperty("name")
            private String name;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ParticipantFrame {
            @JsonProperty("participantId")
            private Integer participantId;

            @JsonProperty("level")
            private Integer level;

            @JsonProperty("currentGold")
            private Integer currentGold;

            @JsonProperty("totalGold")
            private Integer totalGold;

            @JsonProperty("goldPerSecond")
            private Integer goldPerSecond;

            @JsonProperty("minionsKilled")
            private Integer minionsKilled;

            @JsonProperty("jungleMinionsKilled")
            private Integer jungleMinionsKilled;

            @JsonProperty("xp")
            private Integer xp;

            @JsonProperty("timeEnemySpentControlled")
            private Integer timeEnemySpentControlled;

            @JsonProperty("championStats")
            private ChampionStats championStats;

            @JsonProperty("damageStats")
            private DamageStats damageStats;

            @JsonProperty("position")
            private Position position;

            @Getter
            @Setter
            @NoArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ChampionStats {
                private Integer abilityHaste;
                private Integer abilityPower;
                private Integer armor;
                private Integer attackDamage;
                private Integer attackSpeed;
                private Integer health;
                private Integer healthMax;
                private Integer healthRegen;
                private Integer magicResist;
                private Integer movementSpeed;
                private Integer power;
                private Integer powerMax;
                private Integer powerRegen;
            }

            @Getter
            @Setter
            @NoArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class DamageStats {
                @JsonProperty("magicDamageDone")
                private Integer magicDamageDone;

                @JsonProperty("magicDamageDoneToChampions")
                private Integer magicDamageDoneToChampions;

                @JsonProperty("physicalDamageDone")
                private Integer physicalDamageDone;

                @JsonProperty("physicalDamageDoneToChampions")
                private Integer physicalDamageDoneToChampions;

                @JsonProperty("totalDamageDone")
                private Integer totalDamageDone;

                @JsonProperty("totalDamageDoneToChampions")
                private Integer totalDamageDoneToChampions;

                @JsonProperty("totalDamageTaken")
                private Integer totalDamageTaken;

                @JsonProperty("trueDamageDone")
                private Integer trueDamageDone;

                @JsonProperty("trueDamageDoneToChampions")
                private Integer trueDamageDoneToChampions;
            }

            @Getter
            @Setter
            @NoArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Position {
                private Integer x;
                private Integer y;
            }
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ParticipantInfo {
            @JsonProperty("participantId")
            private Integer participantId;

            @JsonProperty("puuid")
            private String puuid;
        }
    }
}
