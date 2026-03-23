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

            /** CHAMPION_KILL: 킬한 플레이어 participantId */
            @JsonProperty("killerId")
            private Integer killerId;

            /** CHAMPION_KILL: 죽은 플레이어 participantId */
            @JsonProperty("victimId")
            private Integer victimId;

            /** CHAMPION_KILL: 어시스트한 플레이어 participantId 목록 */
            @JsonProperty("assistingParticipantIds")
            private List<Integer> assistingParticipantIds;

            /** 이벤트 발생 위치 (동선 분석용) */
            @JsonProperty("position")
            private ParticipantFrame.Position position;

            /** TURRET_PLATE_DESTROYED, BUILDING_KILL: killer 팀 또는 라인 */
            @JsonProperty("teamId")
            private Integer teamId;

            @JsonProperty("laneType")
            private String laneType;

            /** ELITE_MONSTER_KILL: DRAGON, BARON_NASHOR, RIFTHERALD, HORDE */
            @JsonProperty("monsterType")
            private String monsterType;

            /** ELITE_MONSTER_KILL (DRAGON): FIRE_DRAGON, WATER_DRAGON, EARTH_DRAGON, AIR_DRAGON 등 */
            @JsonProperty("monsterSubType")
            private String monsterSubType;

            /** BUILDING_KILL: TOWER_BUILDING, INHIBITOR_BUILDING */
            @JsonProperty("buildingType")
            private String buildingType;

            /** BUILDING_KILL (TOWER): OUTER_TURRET, INNER_TURRET, BASE_TURRET, NEXUS_TURRET */
            @JsonProperty("towerType")
            private String towerType;

            /** DRAGON_SOUL_GIVEN: 영혼 종류 (Mountain, Infernal 등) */
            @JsonProperty("name")
            private String name;

            /** GAME_END: 승리 팀 (100 or 200) */
            @JsonProperty("winningTeam")
            private Integer winningTeam;

            /** LEVEL_UP: 달성 레벨 */
            @JsonProperty("level")
            private Integer level;
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
