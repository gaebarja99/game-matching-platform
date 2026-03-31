package com.gamematcher.dto.search;

import lombok.Builder;
import lombok.Data;

/**
 * 전적 검색 화면에서 매치 상세(전체 JSON) 지연 로드 요청
 */
@Data
@Builder
public class MatchDetailRequest {

    /** lol | tft | valorant | pubg */
    private String game;

    private String matchId;

    /** LoL/TFT 플랫폼 힌트 (예: kr). 미입력 시 kr */
    private String region;

    /** PUBG shard (steam, kakao 등) */
    private String platform;

    /**
     * TFT/LoL 등 DB 캐시 갱신 시 필요할 수 있음. 없으면 상세 응답만 반환.
     */
    private String puuid;

    /**
     * 발로란트·LoL: DB에 저장된 AI 분석 중 붙일 모델 키. 비우면 서버 기본 {@code ai.llm.model}.
     */
    private String llmModel;

    private Boolean forceRefresh;

    public MatchDetailRequest normalize() {
        if (game != null) {
            game = game.trim().toLowerCase();
        }
        if (region == null || region.isBlank()) {
            region = "kr";
        } else {
            region = region.trim().toLowerCase();
        }
        if (platform != null) {
            platform = platform.trim().toLowerCase();
        }
        if (llmModel != null) {
            llmModel = llmModel.trim();
        }
        return this;
    }
}
