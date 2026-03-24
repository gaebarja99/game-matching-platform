package com.gamematcher.dto.valorant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Valorant Lifetime API 전체 응답 래퍼
 * { "status": 200, "results": {...}, "data": [ValorantLifetimeDataItem, ...] }
 */
@Getter
@Setter
@NoArgsConstructor
public class ValorantLifetimeApiResponse {

    private int status;
    private Results results;
    private List<ValorantLifetimeDataItem> data;

    /**
     * Valorant Lifetime API results 페이징 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Results {
        private int total;
        private int returned;
        private int before;
        private int after;
    }
}
