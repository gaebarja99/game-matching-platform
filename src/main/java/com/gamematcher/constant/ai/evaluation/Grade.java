package com.gamematcher.constant.ai.evaluation;

/**
 * 점수 기준: D-~69점, C-70~89점, B-90~119점, A-120~149점, S-150점~
 */
public enum Grade {
    S,  // 150점~
    A,  // 120~149점
    B,  // 90~119점
    C,  // 70~89점
    D;  // ~69점

    public static Grade fromScore(Integer score) {
        if (score == null) {
            return null;
        }
        if (score >= 150) return S;
        else if (score >= 120) return A;
        else if (score >= 90) return B;
        else if (score >= 70) return C;
        else return D;
    }
}
