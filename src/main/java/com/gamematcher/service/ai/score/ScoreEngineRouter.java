package com.gamematcher.service.ai.score;

import com.gamematcher.constant.GameList;
import com.gamematcher.dto.ai.evaluation.BaseStatsDTO;
import com.gamematcher.dto.ai.evaluation.LolStatsDTO;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ScoreEngineRouter {

    private final LolScoreEngine lolEngine;
    private final Optional<GenericScoreEngine> genericEngine;

    public ScoreEngineRouter(LolScoreEngine lolEngine, Optional<GenericScoreEngine> genericEngine) {
        this.lolEngine = lolEngine;
        this.genericEngine = genericEngine;
    }

    public Optional<Integer> calculate(String gameCode, BaseStatsDTO stats) {
        GameList game;
        try {
            game = GameList.valueOf(gameCode);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        return switch (game) {
            case VALORANT -> Optional.empty();
            case LEAGUE_OF_LEGENDS -> stats instanceof LolStatsDTO lolStats
                ? Optional.of(lolEngine.calculate(lolStats))
                : Optional.empty();
            case PUBG -> genericEngine.map(engine -> engine.calculate(stats));
            default -> Optional.empty();
        };
    }
}
