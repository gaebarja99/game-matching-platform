package com.gamematcher.service.ai.score;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.GenericStatsDTO;
import com.gamematcher.dto.ai.evaluation.LolStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantMatchStatsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScoreEngineRouter")
class ScoreEngineRouterTest {

    private final LolScoreEngine lolEngine = new LolScoreEngine();
    private final GenericScoreEngine genericEngine = new GenericScoreEngine();

    private ScoreEngineRouter routerWithGeneric() {
        return new ScoreEngineRouter(lolEngine, Optional.of(genericEngine));
    }

    private ScoreEngineRouter routerWithoutGeneric() {
        return new ScoreEngineRouter(lolEngine, Optional.empty());
    }

    private ValorantMatchStatsDTO sampleValorant() {
        return ValorantMatchStatsDTO.builder()
            .game("VALORANT")
            .kills(10)
            .deaths(5)
            .assists(3)
            .result(MatchResult.VICTORY)
            .roundsWon(13)
            .roundsPlayed(25)
            .headShots(20)
            .totalShots(100)
            .build();
    }

    private LolStatsDTO sampleLol() {
        return new LolStatsDTO("LEAGUE_OF_LEGENDS", 5, 3, 2, MatchResult.VICTORY, 10000, 150, 20000L, 35, 25);
    }

    @Nested
    @DisplayName("VALORANT routing")
    class ValorantRouting {

        @Test
        void valorantReturnsEmpty() {
            ScoreEngineRouter router = routerWithGeneric();
            Optional<Integer> result = router.calculate("VALORANT", sampleValorant());
            assertThat(result).isEmpty();
        }

        @Test
        void typeMismatchForValorant() {
            ScoreEngineRouter router = routerWithGeneric();
            Optional<Integer> result = router.calculate("VALORANT", sampleLol());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("LEAGUE_OF_LEGENDS routing")
    class LolRouting {

        @Test
        void routesToLolEngine() {
            ScoreEngineRouter router = routerWithGeneric();
            Optional<Integer> result = router.calculate("LEAGUE_OF_LEGENDS", sampleLol());
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(lolEngine.calculate(sampleLol()));
        }

        @Test
        void typeMismatchForLol() {
            ScoreEngineRouter router = routerWithGeneric();
            Optional<Integer> result = router.calculate("LEAGUE_OF_LEGENDS", sampleValorant());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("GenericScoreEngine routing")
    class GenericRouting {

        @Test
        void pubgWithGenericEngine() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("PUBG", 5, 2, 1, MatchResult.VICTORY);

            Optional<Integer> result = router.calculate("PUBG", stats);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(genericEngine.calculate(stats));
        }

        @Test
        void pubgWithoutGenericEngine() {
            ScoreEngineRouter router = routerWithoutGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("PUBG", 5, 2, 1, MatchResult.VICTORY);

            Optional<Integer> result = router.calculate("PUBG", stats);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unsupported game")
    class UnsupportedGame {

        @Test
        void overwatch() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("OVERWATCH", 10, 3, 5, MatchResult.VICTORY);

            assertThat(router.calculate("OVERWATCH", stats)).isEmpty();
        }

        @Test
        void cs2() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("COUNTER_STRIKE_2", 20, 5, 0, MatchResult.VICTORY);

            assertThat(router.calculate("COUNTER_STRIKE_2", stats)).isEmpty();
        }

        @Test
        void others() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("OTHERS", 0, 0, 0, MatchResult.DEFEAT);

            assertThat(router.calculate("OTHERS", stats)).isEmpty();
        }

        @Test
        void unknownGameCode() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("UNKNOWN_GAME", 5, 2, 1, MatchResult.VICTORY);

            assertThat(router.calculate("UNKNOWN_GAME", stats)).isEmpty();
        }
    }
}
