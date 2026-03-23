package com.gamematcher.service.ai.score;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.LolStatsDTO;
import org.springframework.stereotype.Component;

/**
 * LoL 전용 점수 엔진.
 * 기준점 100에서 KDA·분당 CS·분당 딜량·시야 점수·승리 여부에 따라 점수를 가감한다.
 * 100 = 평균 수준. 음수·큰 값 모두 허용(클램핑 없음).
 */
@Component
public class LolScoreEngine {

    private static final int BASE_SCORE = 100;

    // KDA 임계값
    private static final double KDA_EXCELLENT    = 5.0;
    private static final double KDA_GOOD         = 3.0;
    private static final double KDA_ABOVE_AVG    = 2.0;
    private static final double KDA_BELOW_AVG    = 1.0;
    private static final double KDA_POOR         = 0.5;

    // KDA 보너스/패널티
    private static final int KDA_BONUS_EXCELLENT  =  40;
    private static final int KDA_BONUS_GOOD       =  20;
    private static final int KDA_BONUS_ABOVE_AVG  =  10;
    private static final int KDA_PENALTY_BELOW    = -20;
    private static final int KDA_PENALTY_POOR     = -35;

    // 분당 CS 임계값
    private static final double CS_PER_MIN_EXCELLENT = 10.0;
    private static final double CS_PER_MIN_GOOD      =  8.0;
    private static final double CS_PER_MIN_AVERAGE   =  6.0;
    private static final double CS_PER_MIN_LOW       =  4.0;

    // 분당 CS 보너스/패널티
    private static final int CS_BONUS_EXCELLENT =  20;
    private static final int CS_BONUS_GOOD      =  10;
    private static final int CS_PENALTY_LOW     = -10;
    private static final int CS_PENALTY_POOR    = -20;

    // 분당 딜량 임계값
    private static final double DAMAGE_PER_MIN_EXCELLENT = 1500.0;
    private static final double DAMAGE_PER_MIN_GOOD      = 1000.0;
    private static final double DAMAGE_PER_MIN_LOW       =  500.0;

    // 분당 딜량 보너스/패널티
    private static final int DAMAGE_BONUS_EXCELLENT =  20;
    private static final int DAMAGE_BONUS_GOOD      =  10;
    private static final int DAMAGE_PENALTY_LOW     = -10;

    // 시야 점수 임계값
    private static final int VISION_EXCELLENT = 50;
    private static final int VISION_GOOD      = 30;
    private static final int VISION_LOW       = 15;

    // 시야 점수 보너스/패널티
    private static final int VISION_BONUS_EXCELLENT =  15;
    private static final int VISION_BONUS_GOOD      =   5;
    private static final int VISION_PENALTY_LOW     =  -5;

    // 승리 보너스
    private static final int WIN_BONUS = 10;

    public int calculate(LolStatsDTO stats) {
        int score = BASE_SCORE;
        score += calcKdaBonus(stats);
        score += calcCsBonus(stats);
        score += calcDamageBonus(stats);
        score += calcVisionBonus(stats);
        if (stats.getResult() == MatchResult.VICTORY) {
            score += WIN_BONUS;
        }
        return score;
    }

    private int calcKdaBonus(LolStatsDTO stats) {
        double kda = (stats.getKills() + stats.getAssists())
                / (double) Math.max(stats.getDeaths(), 1);
        if (kda >= KDA_EXCELLENT) return KDA_BONUS_EXCELLENT;
        if (kda >= KDA_GOOD)      return KDA_BONUS_GOOD;
        if (kda >= KDA_ABOVE_AVG) return KDA_BONUS_ABOVE_AVG;
        if (kda >= KDA_BELOW_AVG) return 0;
        if (kda >= KDA_POOR)      return KDA_PENALTY_BELOW;
        return KDA_PENALTY_POOR;
    }

    private int calcCsBonus(LolStatsDTO stats) {
        int duration = Math.max(stats.getGameDurationMinutes(), 1);
        double csPerMin = (double) stats.getMinionsKilled() / duration;
        if (csPerMin >= CS_PER_MIN_EXCELLENT) return CS_BONUS_EXCELLENT;
        if (csPerMin >= CS_PER_MIN_GOOD)      return CS_BONUS_GOOD;
        if (csPerMin >= CS_PER_MIN_AVERAGE)   return 0;
        if (csPerMin >= CS_PER_MIN_LOW)        return CS_PENALTY_LOW;
        return CS_PENALTY_POOR;
    }

    private int calcDamageBonus(LolStatsDTO stats) {
        int duration = Math.max(stats.getGameDurationMinutes(), 1);
        double damagePerMin = (double) stats.getDamageDealt() / duration;
        if (damagePerMin >= DAMAGE_PER_MIN_EXCELLENT) return DAMAGE_BONUS_EXCELLENT;
        if (damagePerMin >= DAMAGE_PER_MIN_GOOD)      return DAMAGE_BONUS_GOOD;
        if (damagePerMin >= DAMAGE_PER_MIN_LOW)        return 0;
        return DAMAGE_PENALTY_LOW;
    }

    private int calcVisionBonus(LolStatsDTO stats) {
        int vision = stats.getVisionScore();
        if (vision >= VISION_EXCELLENT) return VISION_BONUS_EXCELLENT;
        if (vision >= VISION_GOOD)      return VISION_BONUS_GOOD;
        if (vision >= VISION_LOW)        return 0;
        return VISION_PENALTY_LOW;
    }
}
