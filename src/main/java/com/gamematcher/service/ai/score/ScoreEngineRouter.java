package com.gamematcher.service.ai.score;

import com.gamematcher.constant.GameList;
import com.gamematcher.dto.ai.evaluation.BaseStatsDTO;
import com.gamematcher.dto.ai.evaluation.LolStatsDTO;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 게임 코드로 점수 엔진을 선택하여 점수를 계산한다.
 *
 * <p>switch로 게임별 엔진을 명시적으로 라우팅한다. Map 기반 동적 라우팅 금지.
 * 새 게임을 추가할 때는 반드시 이 클래스의 switch에 case를 추가해야 한다.</p>
 *
 * <p>VALORANT는 라운드 기반 점수만 사용하므로, BaseStatsDTO만으로는 계산 불가.
 * {@link com.gamematcher.service.ai.score.ValorantScoreService}를 직접 호출해야 한다.</p>
 *
 * <p>GenericScoreEngine은 Optional 주입으로 선택적으로 사용한다.
 * PUBG, APEX_LEGENDS 등 명시적으로 등록한 게임에만 적용된다.</p>
 */
@Component
public class ScoreEngineRouter {

    private final LolScoreEngine lolEngine;
    private final Optional<GenericScoreEngine> genericEngine;

    public ScoreEngineRouter(LolScoreEngine lolEngine,
                             Optional<GenericScoreEngine> genericEngine) {
        this.lolEngine = lolEngine;
        this.genericEngine = genericEngine;
    }

    /**
     * 게임 코드에 맞는 엔진으로 점수를 계산한다.
     *
     * @param gameCode {@link GameList} enum 이름 (예: "VALORANT")
     * @param stats    {@link com.gamematcher.dto.ai.evaluation.StatsConverter}로 변환된 DTO.
     *                 gameCode와 동일한 게임 코드로 변환된 DTO여야 함.
     * @return 계산된 점수. 지원하지 않는 게임이거나 DTO 타입 불일치 시 {@code Optional.empty()}.
     */
    public Optional<Integer> calculate(String gameCode, BaseStatsDTO stats) {
        GameList game;
        try {
            game = GameList.valueOf(gameCode);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        return switch (game) {
            case VALORANT -> Optional.empty(); // 라운드 기반만 지원. ValorantScoreService 사용.

            case LEAGUE_OF_LEGENDS -> stats instanceof LolStatsDTO l
                    ? Optional.of(lolEngine.calculate(l))
                    : Optional.empty();

            // GenericScoreEngine으로 명시적으로 등록한 게임
            case PUBG, APEX_LEGENDS -> genericEngine
                    .map(engine -> engine.calculate(stats));

            // 전용 엔진 미등록 게임: 점수 계산 안 함
            default -> Optional.empty();
        };
    }
}
