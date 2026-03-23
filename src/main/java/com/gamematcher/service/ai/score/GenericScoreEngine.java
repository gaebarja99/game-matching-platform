package com.gamematcher.service.ai.score;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.BaseStatsDTO;
import org.springframework.stereotype.Component;

/**
 * 범용 점수 엔진 (선택적).
 * PUBG, APEX_LEGENDS 등 전용 엔진이 없는 게임에 ScoreEngineRouter의 switch case에
 * 명시적으로 등록했을 때만 사용한다. Fallback이 아니며 하위 DTO로 캐스팅하지 않는다.
 * KDA와 승리 여부 등 BaseStatsDTO 공통 필드만으로 점수를 계산한다.
 */
@Component
public class GenericScoreEngine {

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

    // 승리 보너스
    private static final int WIN_BONUS = 10;

    public int calculate(BaseStatsDTO stats) {
        int score = BASE_SCORE;
        score += calcKdaBonus(stats);
        if (stats.getResult() == MatchResult.VICTORY) {
            score += WIN_BONUS;
        }
        return score;
    }

    private int calcKdaBonus(BaseStatsDTO stats) {
        double kda = (stats.getKills() + stats.getAssists())
                / (double) Math.max(stats.getDeaths(), 1);
        if (kda >= KDA_EXCELLENT) return KDA_BONUS_EXCELLENT;
        if (kda >= KDA_GOOD)      return KDA_BONUS_GOOD;
        if (kda >= KDA_ABOVE_AVG) return KDA_BONUS_ABOVE_AVG;
        if (kda >= KDA_BELOW_AVG) return 0;
        if (kda >= KDA_POOR)      return KDA_PENALTY_BELOW;
        return KDA_PENALTY_POOR;
    }
}
