package com.gamematcher.dto.lol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * LoL 매치 상세 API 응답 DTO (Riot Match-v5)
 * 단일 매치 JSON 구조
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LolMatchDetailDto {

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

    // --- info (Riot Match-v5 API info) ---
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        @JsonProperty("gameId")
        private Long gameId;

        @JsonProperty("gameCreation")
        private Long gameCreation;

        @JsonProperty("gameDuration")
        private Long gameDuration;

        @JsonProperty("gameStartTimestamp")
        private Long gameStartTimestamp;

        @JsonProperty("gameEndTimestamp")
        private Long gameEndTimestamp;

        @JsonProperty("gameMode")
        private String gameMode;

        @JsonProperty("gameType")
        private String gameType;

        @JsonProperty("gameName")
        private String gameName;

        @JsonProperty("gameVersion")
        private String gameVersion;

        @JsonProperty("mapId")
        private Integer mapId;

        @JsonProperty("queueId")
        private Integer queueId;

        @JsonProperty("platformId")
        private String platformId;

        @JsonProperty("endOfGameResult")
        private String endOfGameResult;

        @JsonProperty("tournamentCode")
        private String tournamentCode;

        private List<LolParticipantDto> participants;

        private List<Team> teams;

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Team {
            @JsonProperty("teamId")
            private Integer teamId;

            @JsonProperty("win")
            private Boolean win;

            private List<Ban> bans;

            private Map<String, Objective> objectives;

            @JsonProperty("feats")
            private Map<String, Object> feats;

            @Getter
            @Setter
            @NoArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Ban {
                @JsonProperty("championId")
                private Integer championId;

                @JsonProperty("pickTurn")
                private Integer pickTurn;
            }

            @Getter
            @Setter
            @NoArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Objective {
                @JsonProperty("first")
                private Boolean first;

                @JsonProperty("kills")
                private Integer kills;
            }
        }
    }
}
