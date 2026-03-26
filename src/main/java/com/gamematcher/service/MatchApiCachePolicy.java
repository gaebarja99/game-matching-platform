package com.gamematcher.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 전적 검색 시 외부 API 응답을 DB에 캐시할 때의 신선도 기준.
 */
public final class MatchApiCachePolicy {

    public static final Duration STALE_AFTER = Duration.ofDays(1);

    private MatchApiCachePolicy() {
    }

    /** null 이거나 기준 시각보다 오래되었으면 API로 다시 받아 갱신한다. */
    public static boolean isStale(LocalDateTime apiCachedAt) {
        if (apiCachedAt == null) {
            return true;
        }
        return apiCachedAt.isBefore(LocalDateTime.now().minus(STALE_AFTER));
    }
}
