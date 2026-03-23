package com.gamematcher.dto.valorant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Valorant MMR API 응답 (Henrik /valorant/v2/by-puuid/mmr/{region}/{puuid})
 * { "status": 200, "data": { name, tag, puuid, current_data, highest_rank, by_season } }
 */
@Getter
@Setter
@NoArgsConstructor
public class ValorantMmrApiResponse {

    private int status;
    private MmrData data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MmrData {
        private String name;
        private String tag;
        private String puuid;

        @JsonProperty("current_data")
        private CurrentData currentData;

        @JsonProperty("highest_rank")
        private HighestRank highestRank;

        @JsonProperty("by_season")
        private Map<String, Object> bySeason = new LinkedHashMap<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CurrentData {
        private Integer currenttier;

        @JsonProperty("currenttierpatched")
        private String currentTierPatched;

        private Images images;

        @JsonProperty("ranking_in_tier")
        private Integer rankingInTier;

        @JsonProperty("mmr_change_to_last_game")
        private Integer mmrChangeToLastGame;

        private Integer elo;

        @JsonProperty("games_needed_for_rating")
        private Integer gamesNeededForRating;

        private Boolean old;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Images {
        private String small;
        private String large;

        @JsonProperty("triangle_down")
        private String triangleDown;

        @JsonProperty("triangle_up")
        private String triangleUp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HighestRank {
        private Boolean old;
        private Integer tier;

        @JsonProperty("patched_tier")
        private String patchedTier;

        private String season;
    }
}
