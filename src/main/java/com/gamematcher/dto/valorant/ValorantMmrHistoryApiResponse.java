package com.gamematcher.dto.valorant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Valorant MMR History API 응답 (Henrik /valorant/v1/by-puuid/mmr-history/{region}/{puuid})
 * { "status": 200, "name", "tag", "data": [ {...}, ... ] }
 */
@Getter
@Setter
@NoArgsConstructor
public class ValorantMmrHistoryApiResponse {

    private int status;
    private String name;
    private String tag;
    private List<MmrHistoryItem> data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MmrHistoryItem {
        private Integer currenttier;

        @JsonProperty("currenttierpatched")
        private String currentTierPatched;

        private Images images;

        @JsonProperty("match_id")
        private String matchId;

        private MapRef map;

        @JsonProperty("season_id")
        private String seasonId;

        @JsonProperty("ranking_in_tier")
        private Integer rankingInTier;

        @JsonProperty("mmr_change_to_last_game")
        private Integer mmrChangeToLastGame;

        private Integer elo;
        private String date;

        @JsonProperty("date_raw")
        private Long dateRaw;
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
    public static class MapRef {
        private String id;
        private String name;
    }
}
