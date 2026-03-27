package com.gamematcher.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PlayerSearchResponse {

    private boolean success;
    private String errorMessage;
    private String game;
    private String nickname;
    /** true면 matches 행은 matchId 위주이며 요약(KDA 등)은 없음 — 상세는 match-detail API */
    private Boolean matchListOnly;
    /** 발로란트: 티어는 아직 로드 전 — {@code POST /api/search/valorant/mmr} 로 채움 */
    private Boolean valorantMmrPending;
    private PlayerInfo playerInfo;
    private List<MatchInfo> matches;
    private MatchStats stats;

    @Data
    @Builder
    public static class PlayerInfo {
        private String puuid;
        private String gameName;
        private String tagLine;
        private String summonerLevel;
        private String profileIconId;
        private String tier;
        private String rank;
        private String lp;
        private String steamId;
        private String avatarUrl;
        private Map<String, Object> rawData;
    }

    @Data
    @Builder
    public static class MatchInfo {
        private String matchId;
        private String gameMode;
        private String champion;
        private String agent;
        private Boolean win;
        private Integer kills;
        private Integer deaths;
        private Integer assists;
        private Double kda;
        private Integer cs;
        private Integer playtime;
        private String playedAt;
        private Map<String, Object> extras;
    }

    @Data
    @Builder
    public static class MatchStats {
        private Integer totalGames;
        private Integer wins;
        private Integer losses;
        private Double winRate;
        private Double avgKills;
        private Double avgDeaths;
        private Double avgAssists;
        private Double avgKda;
        private String mostUsedChampionOrAgent;
    }

    public static PlayerSearchResponse error(String game, String nickname, String message) {
        return PlayerSearchResponse.builder()
                .success(false)
                .game(game)
                .nickname(nickname)
                .errorMessage(message)
                .build();
    }
}
