package com.gamematcher.constant.ai.evaluation;

/**
 * 발로란트 라운드 점수 계산 상수.
 * {@link com.gamematcher.service.ai.score.ValorantRoundScoreEngine}에서 사용.
 */
public final class ValorantRoundScoreConstants {

    private ValorantRoundScoreConstants() {}

    // --- 점수·가중치 ---
    public static final int BASE_SCORE = 100;
    public static final int KILL_BASE = 15;
    public static final double ASSIST_RATIO = 0.4;
    public static final double ASSIST_DAMAGE_RATIO_CAP = 1.2;
    public static final int DAMAGE_SCORE_PER_100 = 8;
    public static final double KILL_DAMAGE_RATIO_MIN = 0.6;

    // --- 경제 ---
    public static final int ECONOMY_FLOOR = 1500;
    public static final double ECONOMY_MULTIPLIER_CAP = 1.5;
    public static final double ECONOMY_MULTIPLIER_MIN = 0.8;

    // --- 상황 변화 배율 ---
    public static final double SITUATION_EQUALIZE = 1.4;       // 열세→동률 (3v4→3v3 등)
    public static final double SITUATION_STILL_BEHIND = 1.1;   // 열세→여전히 열세 (1v3→1v2)
    public static final double SITUATION_TAKE_LEAD = 1.2;      // 동률→우세 (2v2→2v1)
    public static final double SITUATION_CLEANUP = 0.75;       // 클린업 (4v1→4v0 등)
    public static final double SITUATION_FIRST_KILL = 1.35;    // 5v5→5v4 FB
    public static final double SITUATION_DEFAULT = 1.0;

    // --- 보너스 ---
    public static final int CLUTCH_BONUS = 45;
    public static final int ROUND_WIN_BONUS = 15;

    // --- 사망 패널티 ---
    public static final int DEATH_PENALTY_TRADE = 0;
    public static final int DEATH_PENALTY_5V5_LONE = -25;
    public static final int DEATH_PENALTY_FIRST_BLOOD_LOST = -25;
    public static final int DEATH_PENALTY_ECO_DISADVANTAGE = -10;
    public static final int DEATH_PENALTY_DEFAULT = -20;

    // --- 트레이드·기타 ---
    public static final long TRADE_WINDOW_MS = 4000;
    public static final int AFK_PENALTY = -100;
    public static final int PENALIZED_PENALTY = -100;

    // --- 생존 수 상한 (세이지 부활) ---
    public static final int ALIVE_CAP = 5;
}
