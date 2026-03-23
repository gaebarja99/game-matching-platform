package com.gamematcher.service.ai;

import com.gamematcher.constant.ai.evaluation.Grade;
import com.gamematcher.dto.ai.evaluation.BaseStatsDTO;
import com.gamematcher.service.ai.score.ScoreEngineRouter;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 규칙 기반 점수·등급 서비스.
 * ScoreEngineRouter에서 점수를 받아 Grade로 변환하는 단일 책임만 가진다.
 * LLM 호출 없이 규칙 기반으로만 평가가 완성된다.
 */
@Service
public class RuleBasedScoreService {

    private final ScoreEngineRouter router;

    public RuleBasedScoreService(ScoreEngineRouter router) {
        this.router = router;
    }

    /**
     * 게임별 엔진으로 점수를 계산한다.
     *
     * @return 점수. 지원하지 않는 게임이거나 DTO 타입 불일치 시 {@code Optional.empty()}.
     */
    public Optional<Integer> calculateScore(String gameCode, BaseStatsDTO stats) {
        return router.calculate(gameCode, stats);
    }

    /**
     * 게임별 엔진으로 점수를 계산한 뒤 {@link Grade}로 변환한다.
     *
     * @return 등급. 점수 계산이 불가하면 {@code Optional.empty()}.
     */
    public Optional<Grade> calculateGrade(String gameCode, BaseStatsDTO stats) {
        return calculateScore(gameCode, stats)
                .map(Grade::fromScore);
    }
}
