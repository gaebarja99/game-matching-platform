package com.gamematcher.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 매치 상세 지연 로드 응답 (게임 원본 구조에 가까운 Map)
 */
@Data
@Builder
public class MatchDetailResponse {

    private boolean success;
    private String errorMessage;
    private String game;
    private String matchId;
    private Map<String, Object> payload;

    public static MatchDetailResponse error(String game, String matchId, String message) {
        return MatchDetailResponse.builder()
                .success(false)
                .game(game)
                .matchId(matchId)
                .errorMessage(message)
                .build();
    }
}
