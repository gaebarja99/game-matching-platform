package com.gamematcher.dto.valorant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant 매치 메타데이터 DTO
 * API 응답의 metadata 필드 구조
 */
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatchInfoDto {

    private String map;

    @JsonProperty("game_version")
    private String gameVersion;

    @JsonProperty("game_length")
    private int gameLength;

    @JsonProperty("game_start")
    private long gameStart;

    @JsonProperty("game_start_patched")
    private String gameStartPatched;

    @JsonProperty("rounds_played")
    private int roundsPlayed;

    private String mode;

    @JsonProperty("mode_id")
    private String modeId;

    private String queue;

    @JsonProperty("season_id")
    private String seasonId;

    private String platform;

    @JsonProperty("matchid")
    private String matchId;

    @JsonProperty("premier_info")
    private PremierInfo premierInfo;

    private String region;

    private String cluster;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PremierInfo {
        @JsonProperty("tournament_id")
        private String tournamentId;

        @JsonProperty("matchup_id")
        private String matchupId;
    }
}
