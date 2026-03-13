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

    /** GenericScoreEngine 빈이 있는 라우터 */
    private ScoreEngineRouter routerWithGeneric() {
        return new ScoreEngineRouter(lolEngine, Optional.of(genericEngine));
    }

    /** GenericScoreEngine 빈이 없는 라우터 */
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
    @DisplayName("VALORANT 라우팅")
    class ValorantRouting {

        @Test
        @DisplayName("VALORANT → empty (라운드 기반만 지원, ValorantScoreService 직접 사용)")
        void valorantReturnsEmpty() {
            ScoreEngineRouter router = routerWithGeneric();
            Optional<Integer> result = router.calculate("VALORANT", sampleValorant());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("LolStatsDTO를 VALORANT 코드로 요청 → empty (타입 불일치)")
        void typeMismatchForValorant() {
            ScoreEngineRouter router = routerWithGeneric();
            Optional<Integer> result = router.calculate("VALORANT", sampleLol());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("LEAGUE_OF_LEGENDS 라우팅")
    class LolRouting {

        @Test
        @DisplayName("LolStatsDTO → 점수 반환")
        void routesToLolEngine() {
            ScoreEngineRouter router = routerWithGeneric();
            Optional<Integer> result = router.calculate("LEAGUE_OF_LEGENDS", sampleLol());

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(lolEngine.calculate(sampleLol()));
        }

        @Test
        @DisplayName("ValorantStatsDTO를 LEAGUE_OF_LEGENDS 코드로 요청 → empty (타입 불일치)")
        void typeMismatchForLol() {
            ScoreEngineRouter router = routerWithGeneric();
            Optional<Integer> result = router.calculate("LEAGUE_OF_LEGENDS", sampleValorant());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("GenericScoreEngine 라우팅 (PUBG, APEX_LEGENDS)")
    class GenericRouting {

        @Test
        @DisplayName("PUBG + GenericEngine 빈 존재 → 점수 반환")
        void pubgWithGenericEngine() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("PUBG", 5, 2, 1, MatchResult.VICTORY);

            Optional<Integer> result = router.calculate("PUBG", stats);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(genericEngine.calculate(stats));
        }

        @Test
        @DisplayName("APEX_LEGENDS + GenericEngine 빈 존재 → 점수 반환")
        void apexWithGenericEngine() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("APEX_LEGENDS", 3, 1, 2, MatchResult.DEFEAT);

            Optional<Integer> result = router.calculate("APEX_LEGENDS", stats);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("PUBG + GenericEngine 빈 없음 → empty")
        void pubgWithoutGenericEngine() {
            ScoreEngineRouter router = routerWithoutGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("PUBG", 5, 2, 1, MatchResult.VICTORY);

            Optional<Integer> result = router.calculate("PUBG", stats);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("미지원 게임 → empty")
    class UnsupportedGame {

        @Test
        @DisplayName("OVERWATCH → empty")
        void overwatch() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("OVERWATCH", 10, 3, 5, MatchResult.VICTORY);

            assertThat(router.calculate("OVERWATCH", stats)).isEmpty();
        }

        @Test
        @DisplayName("COUNTER_STRIKE_2 → empty")
        void cs2() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("COUNTER_STRIKE_2", 20, 5, 0, MatchResult.VICTORY);

            assertThat(router.calculate("COUNTER_STRIKE_2", stats)).isEmpty();
        }

        @Test
        @DisplayName("OTHERS → empty")
        void others() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("OTHERS", 0, 0, 0, MatchResult.DEFEAT);

            assertThat(router.calculate("OTHERS", stats)).isEmpty();
        }

        @Test
        @DisplayName("GameList에 없는 코드(오타 등) → empty")
        void unknownGameCode() {
            ScoreEngineRouter router = routerWithGeneric();
            GenericStatsDTO stats = new GenericStatsDTO("UNKNOWN_GAME", 5, 2, 1, MatchResult.VICTORY);

            assertThat(router.calculate("UNKNOWN_GAME", stats)).isEmpty();
        }
    }
}
