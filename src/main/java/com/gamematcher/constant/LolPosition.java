package com.gamematcher.constant;

import java.util.List;

/**
 * 희망 포지션. FLEX(상관없음)는 빈 라인에 배치됩니다.
 */
public enum LolPosition {
    TOP,
    JUNGLE,
    MID,
    ADC,
    SUPPORT,
    FLEX;

    /** 실제 라인 5개 (FLEX 제외) */
    public static final List<LolPosition> LANES = List.of(TOP, JUNGLE, MID, ADC, SUPPORT);

    public boolean isLane() {
        return this != FLEX;
    }

    public static LolPosition fromClient(String raw) {
        if (raw == null || raw.isBlank()) return FLEX;
        try {
            return LolPosition.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FLEX;
        }
    }
}
