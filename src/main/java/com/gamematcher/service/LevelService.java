package com.gamematcher.service;

/**
 * 레벨 1~9999. 레벨 1 = 0 XP, 1000 XP 도달 시 레벨 2.
 * 레벨별 필요 경험치: 1→2 1000, 2→3 1500, 3→4 2000, ... (레벨 N → N+1: 1000 + (N-1)*500 XP).
 * 경험치는 0.1 단위로 저장 (totalExperienceTenths: 1 XP = 10).
 */
public final class LevelService {

    private static final int MAX_LEVEL = 9999;

    /**
     * 레벨 L에 도달하기 위해 필요한 누적 경험치(0.1 단위).
     * L=1 → 0, L=2 → 10000 (1000 XP), L=3 → 25000 (2500 XP), L=4 → 45000 ...
     */
    public static long getTotalExperienceTenthsForLevel(int level) {
        if (level <= 1) return 0L;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        // L레벨 도달 = sum(1 to L-1 of (1000 + (i-1)*500)) = (L-1)*1000 + 500*(0+1+...+(L-2))
        // = (L-1)*1000 + 500*(L-2)(L-1)/2 = (L-1) * (1000 + 250*(L-2)) = (L-1)*(500 + 250*L)
        // in tenths: (L-1) * (10000 + 2500*L) / 10? No. 1000 XP = 10000 tenths. So (L-1)*1000 + 500*(L-2)(L-1)/2 in XP, in tenths * 10.
        long xp = (long) (level - 1) * 1000L + 500L * (level - 2) * (level - 1) / 2;
        return xp * 10;
    }

    /**
     * 현재 누적 경험치(0.1 단위)로 레벨 계산 (1 ~ 9999).
     */
    public static int getLevel(long totalExperienceTenths) {
        if (totalExperienceTenths <= 0) return 1;
        for (int L = MAX_LEVEL; L >= 1; L--) {
            if (totalExperienceTenths >= getTotalExperienceTenthsForLevel(L)) {
                return L;
            }
        }
        return 1;
    }

    /**
     * 현재 레벨에서 다음 레벨까지 필요한 경험치(0.1 단위). 레벨 1이면 1000 XP = 10000 tenths.
     */
    public static long getExperienceRequiredForNextLevel(int level) {
        if (level < 1 || level >= MAX_LEVEL) return getTotalExperienceTenthsForLevel(2); // 10000
        return getTotalExperienceTenthsForLevel(level + 1) - getTotalExperienceTenthsForLevel(level);
    }

    /**
     * 현재 레벨 내에서 쌓은 경험치(0.1 단위). (현재 총 경험치 - 현재 레벨 도달 경험치)
     */
    public static long getExperienceInCurrentLevel(long totalExperienceTenths, int level) {
        long base = getTotalExperienceTenthsForLevel(level);
        return Math.max(0, totalExperienceTenths - base);
    }
}
