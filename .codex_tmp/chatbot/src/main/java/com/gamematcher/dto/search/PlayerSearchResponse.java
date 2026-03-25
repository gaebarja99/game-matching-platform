package com.gamematcher.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 전적 검색 통합 응답 DTO
 */
@Data
@Builder
public class PlayerSearchResponse {

    /** 검색 성공 여부 */
    private boolean success;

    /** 에러 메시지 (실패 시) */
    private String errorMessage;

    /** 검색한 게임 */
    private String game;

    /** 검색한 닉네임 */
    private String nickname;

    /** 플레이어 기본 정보 */
    private PlayerInfo playerInfo;

    /** 최근 매치 목록 */
    private List<MatchInfo> matches;

    /** 요약 통계 */
    private MatchStats stats;

    // ──────────────────────────────────────────────
    // 중첩 클래스들
    // ──────────────────────────────────────────────

    @Data
    @Builder
    public static class PlayerInfo {
        private String puuid;
        private String gameName;
        private String tagLine;
        private String summonerLevel;
        private String profileIconId;
        private String tier;          // 솔로랭크 티어 (LoL)
        private String rank;
        private String lp;
        private String steamId;
        private String avatarUrl;
        private Map<String, Object> rawData; // 원본 API 응답 일부
    }

    @Data
    @Builder
    public static class MatchInfo {
        private String matchId;
        private String gameMode;
        private String champion;      // LoL 챔피언명
        private String agent;         // Valorant 요원명
        private Boolean win;
        private Integer kills;
        private Integer deaths;
        private Integer assists;
        private Double kda;
        private Integer cs;           // LoL CS
        private Integer playtime;     // 초 단위
        private String playedAt;
        private Map<String, Object> extras; // 게임별 추가 정보
    }

    @Data
    @Builder
    public static class MatchStats {
        private int totalGames;
        private int wins;
        private int losses;
        private double winRate;
        private double avgKills;
        private double avgDeaths;
        private double avgAssists;
        private double avgKda;
        private String mostUsedChampionOrAgent;
    }

    // ──────────────────────────────────────────────
    // 팩토리 메서드
    // ──────────────────────────────────────────────

    public static PlayerSearchResponse error(String game, String nickname, String message) {
        return PlayerSearchResponse.builder()
                .success(false)
                .game(game)
                .nickname(nickname)
                .errorMessage(message)
                .build();
    }
}
