package com.gamematcher.service.ai.score;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.ValorantMatchStatsDTO;
import org.springframework.stereotype.Component;

/**
 * 발로란트 전용 점수 엔진.
 * 기준점 100에서 KDA·라운드 승률·헤드샷율·승리 여부에 따라 점수를 가감한다.
 * 100 = 평균 수준. 음수·큰 값 모두 허용(클램핑 없음).
 */
@Component
public class ValorantScoreEngine {

    private static final int BASE_SCORE = 100;

    // KDA 임계값
    private static final double KDA_EXCELLENT    = 5.0;
    private static final double KDA_GOOD         = 3.0;
    private static final double KDA_ABOVE_AVG    = 2.0;
    private static final double KDA_BELOW_AVG    = 1.0;
    private static final double KDA_POOR         = 0.5;

    // KDA 보너스/패널티
    private static final int KDA_BONUS_EXCELLENT  =  50;
    private static final int KDA_BONUS_GOOD       =  25;
    private static final int KDA_BONUS_ABOVE_AVG  =  10;
    private static final int KDA_PENALTY_BELOW    = -20;
    private static final int KDA_PENALTY_POOR     = -35;

    // 라운드 승률 임계값
    private static final double RWR_HIGH = 0.60;
    private static final double RWR_LOW  = 0.40;

    // 라운드 승률 보너스/패널티
    private static final int RWR_BONUS_HIGH  =  15;
    private static final int RWR_PENALTY_LOW = -10;

    // 헤드샷율 임계값
    private static final double HSR_EXCELLENT = 0.30;
    private static final double HSR_GOOD      = 0.20;
    private static final double HSR_AVERAGE   = 0.10;

    // 헤드샷율 보너스/패널티
    private static final int HSR_BONUS_EXCELLENT =  20;
    private static final int HSR_BONUS_GOOD      =  10;
    private static final int HSR_PENALTY_LOW     =  -5;

    // 승리 보너스
    private static final int WIN_BONUS = 10;

    public int calculate(ValorantMatchStatsDTO stats) {
        int score = BASE_SCORE;
        score += calcKdaBonus(stats);
        score += calcRoundWinRateBonus(stats);
        score += calcHeadshotRateBonus(stats);
        if (stats.getResult() == MatchResult.VICTORY) {
            score += WIN_BONUS;
        }
        return score;
    }

    private int calcKdaBonus(ValorantMatchStatsDTO stats) {
        double kda = (stats.getKills() + stats.getAssists())
                / (double) Math.max(stats.getDeaths(), 1);
        if (kda >= KDA_EXCELLENT) return KDA_BONUS_EXCELLENT;
        if (kda >= KDA_GOOD)      return KDA_BONUS_GOOD;
        if (kda >= KDA_ABOVE_AVG) return KDA_BONUS_ABOVE_AVG;
        if (kda >= KDA_BELOW_AVG) return 0;
        if (kda >= KDA_POOR)      return KDA_PENALTY_BELOW;
        return KDA_PENALTY_POOR;
    }

    private int calcRoundWinRateBonus(ValorantMatchStatsDTO stats) {
        if (stats.getRoundsPlayed() <= 0) return 0;
        double rate = (double) stats.getRoundsWon() / stats.getRoundsPlayed();
        if (rate >= RWR_HIGH) return RWR_BONUS_HIGH;
        if (rate >= RWR_LOW)  return 0;
        return RWR_PENALTY_LOW;
    }

    private int calcHeadshotRateBonus(ValorantMatchStatsDTO stats) {
        if (stats.getTotalShots() <= 0) return 0;
        double rate = (double) stats.getHeadShots() / stats.getTotalShots();
        if (rate >= HSR_EXCELLENT) return HSR_BONUS_EXCELLENT;
        if (rate >= HSR_GOOD)      return HSR_BONUS_GOOD;
        if (rate >= HSR_AVERAGE)   return 0;
        return HSR_PENALTY_LOW;
    }
}
