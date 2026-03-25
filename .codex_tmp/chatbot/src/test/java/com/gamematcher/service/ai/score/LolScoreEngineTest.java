package com.gamematcher.service.ai.score;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.LolStatsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LolScoreEngine")
class LolScoreEngineTest {

    private LolScoreEngine engine;

    @BeforeEach
    void setUp() {
        engine = new LolScoreEngine();
    }

    /** KDA=1.5 → +0, CS/min=7 → +10, damage/min=800 → +10, vision=20 → 0 */
    private LolStatsDTO average(MatchResult result) {
        return new LolStatsDTO("LEAGUE_OF_LEGENDS",
                6, 4, 0, result,
                8000, 140, 16000L, 20, 20);
    }

    @Nested
    @DisplayName("KDA 보너스/패널티")
    class KdaTest {

        @Test
        @DisplayName("KDA >= 5 → +40")
        void kdaExcellent() {
            // kills=10, assists=5, deaths=1 → KDA=15
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 10, 1, 5, MatchResult.DEFEAT, 0, 0, 0L, 0, 1);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 40 - 20 - 10 - 5);
        }

        @Test
        @DisplayName("KDA < 0.5 → -35")
        void kdaPoor() {
            // kills=0, deaths=10, assists=0 → KDA=0
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 0, 10, 0, MatchResult.DEFEAT, 0, 0, 0L, 0, 1);
            int score = engine.calculate(stats);
            assertThat(score).isLessThan(100);
            assertThat(score).isEqualTo(100 - 35 - 20 - 10 - 5);
        }

        @Test
        @DisplayName("deaths=0이면 deaths=1로 처리 (0 나누기 방지)")
        void deathsZero() {
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 5, 0, 0, MatchResult.DEFEAT, 0, 0, 0L, 0, 1);
            // KDA=5.0 → +40
            assertThat(engine.calculate(stats)).isEqualTo(100 + 40 - 20 - 10 - 5);
        }
    }

    @Nested
    @DisplayName("분당 CS 보너스/패널티")
    class CsTest {

        @Test
        @DisplayName("분당 CS >= 10 → +20")
        void csExcellent() {
            // 200cs / 20min = 10
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 200, 0L, 0, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 20 - 10 - 5);  // dmg<500→-10, vision<15→-5
        }

        @Test
        @DisplayName("분당 CS < 4 → -20")
        void csPoor() {
            // 60cs / 20min = 3
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 60, 0L, 0, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 20 - 10 - 5);
        }

        @Test
        @DisplayName("gameDurationMinutes=0이면 1분으로 처리 (0 나누기 방지)")
        void durationZero() {
            // minions=10, duration=0→1 → CS/min=10 → +20
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 10, 0L, 0, 0);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 20 - 10 - 5);
        }
    }

    @Nested
    @DisplayName("분당 딜량 보너스/패널티")
    class DamageTest {

        @Test
        @DisplayName("분당 딜량 >= 1500 → +20")
        void damageExcellent() {
            // 30000 / 20min = 1500
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 0, 30000L, 0, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 20 - 20 - 5);  // CS<4→-20, vision<15→-5
        }

        @Test
        @DisplayName("분당 딜량 < 500 → -10")
        void damageLow() {
            // 5000 / 20min = 250
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 0, 5000L, 0, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 10 - 20 - 5);
        }

        @Test
        @DisplayName("gameDurationMinutes=0이면 1분으로 처리 (0 나누기 방지)")
        void durationZeroForDamage() {
            // duration=0→1, damage=2000/1=2000≥1500→+20, CS=0/1=0<4→-20, vision=0<15→-5
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 0, 2000L, 0, 0);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 20 + 20 - 5);
        }
    }

    @Nested
    @DisplayName("시야 점수 보너스/패널티")
    class VisionTest {

        @Test
        @DisplayName("시야 >= 50 → +15")
        void visionExcellent() {
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 0, 0L, 50, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 15 - 20 - 10);
        }

        @Test
        @DisplayName("시야 >= 30 → +5")
        void visionGood() {
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 0, 0L, 30, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 5 - 20 - 10);
        }

        @Test
        @DisplayName("시야 >= 15 → 0")
        void visionAvg() {
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 0, 0L, 15, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 20 - 10);
        }

        @Test
        @DisplayName("시야 < 15 → -5")
        void visionLow() {
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 3, 3, 0, MatchResult.DEFEAT, 0, 0, 0L, 10, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 20 - 10 - 5);
        }
    }

    @Nested
    @DisplayName("승리 보너스")
    class WinBonusTest {

        @Test
        @DisplayName("승리 시 +10")
        void won() {
            int wonScore = engine.calculate(average(MatchResult.VICTORY));
            int lostScore = engine.calculate(average(MatchResult.DEFEAT));
            assertThat(wonScore - lostScore).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("극단값")
    class EdgeCaseTest {

        @Test
        @DisplayName("모든 스탯 0 → 점수는 기준점보다 낮음")
        void allZeroStats() {
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS", 0, 0, 0, MatchResult.DEFEAT, 0, 0, 0L, 0, 0);
            int score = engine.calculate(stats);
            // KDA=0 < 0.5 → -35, CS/min(duration=1)=0 < 4 → -20, dmg/min=0 < 500 → -10, vision=0 < 15 → -5
            assertThat(score).isEqualTo(100 - 35 - 20 - 10 - 5);
        }

        @Test
        @DisplayName("퍼펙트 게임 → 높은 점수")
        void perfectGame() {
            // KDA=5+→+40, CS/min=12→+20, dmg/min=2000→+20, vision=60→+15, win→+10
            LolStatsDTO stats = new LolStatsDTO("LEAGUE_OF_LEGENDS",
                    20, 0, 10, MatchResult.VICTORY,
                    15000, 240, 40000L, 60, 20);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 40 + 20 + 20 + 15 + 10);
        }
    }
}
