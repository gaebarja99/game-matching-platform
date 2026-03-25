package com.gamematcher.constant;

/**
 * 리그 오브 레전드 랭크 티어 (아이언 ~ 챌린저).
 * ordinal 간격 1이 곧 "상·하위 1단계" 확장에 사용됩니다.
 */
public enum LolTier {
    IRON,
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    EMERALD,
    DIAMOND,
    MASTER,
    GRANDMASTER,
    CHALLENGER;

    public static LolTier clamp(int ordinal) {
        LolTier[] v = values();
        if (ordinal < 0) return v[0];
        if (ordinal >= v.length) return v[v.length - 1];
        return v[ordinal];
    }

    public LolTier minusOne() {
        return clamp(ordinal() - 1);
    }

    public LolTier plusOne() {
        return clamp(ordinal() + 1);
    }
}
