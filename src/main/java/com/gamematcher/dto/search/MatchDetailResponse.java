package com.gamematcher.dto.search;

import com.gamematcher.util.RateLimitMessageUtil;
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
        return error(game, matchId, message, null);
    }

    public static MatchDetailResponse error(String game, String matchId, String message, Throwable cause) {
        String m = (message == null || message.isBlank()) ? "매치 정보를 불러오지 못했습니다." : message;
        m = RateLimitMessageUtil.toUserMessage(m, cause);
        return MatchDetailResponse.builder()
                .success(false)
                .game(game)
                .matchId(matchId)
                .errorMessage(m)
                .build();
    }
}
