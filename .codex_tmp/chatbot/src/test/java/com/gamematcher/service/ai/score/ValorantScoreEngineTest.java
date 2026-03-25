package com.gamematcher.service.ai.score;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.ValorantMatchStatsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 각 보너스/패널티를 독립적으로 검증하기 위해 나머지 요소는 아래 중립값을 사용한다.
 * - KDA 중립 (보너스=0): kills=3, deaths=3, assists=0 → KDA=1.0 (in [1.0, 2.0))
 * - RWR 중립 (보너스=0): roundsWon=10, roundsPlayed=25 → 0.40 (≥ 0.40)
 * - HSR 중립 (보너스=0): headshots=10, totalShots=100 → 0.10 (≥ 0.10)
 * - 패배: won=false
 * 모든 중립값 조합 시 기준 점수 = 100.
 */
@DisplayName("ValorantScoreEngine")
class ValorantScoreEngineTest {

    private ValorantScoreEngine engine;

    // 중립값 상수
    private static final int N_KILLS = 3, N_DEATHS = 3, N_ASSISTS = 0;
    private static final int N_RWR_WON = 10, N_RWR_PLAYED = 25;
    private static final int N_HS = 10, N_SHOTS = 100;

    @BeforeEach
    void setUp() {
        engine = new ValorantScoreEngine();
    }

    private ValorantMatchStatsDTO dto(int kills, int deaths, int assists, MatchResult result,
                                 int roundsWon, int roundsPlayed,
                                 int headshots, int totalShots) {
        return ValorantMatchStatsDTO.builder()
                .game("VALORANT")
                .kills(kills)
                .deaths(deaths)
                .assists(assists)
                .result(result)
                .roundsWon(roundsWon)
                .roundsPlayed(roundsPlayed)
                .headShots(headshots)
                .totalShots(totalShots)
                .build();
    }

    @Nested
    @DisplayName("KDA 보너스/패널티 (RWR·HSR 중립)")
    class KdaTest {

        @Test
        @DisplayName("KDA >= 5 → +50")
        void kdaExcellent() {
            // KDA = (15+10)/1 = 25
            var stats = dto(15, 1, 10, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 50);
        }

        @Test
        @DisplayName("KDA >= 3 → +25")
        void kdaGood() {
            // KDA = (6+3)/2 = 4.5
            var stats = dto(6, 2, 3, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 25);
        }

        @Test
        @DisplayName("KDA >= 2 → +10")
        void kdaAboveAvg() {
            // KDA = (4+0)/2 = 2.0
            var stats = dto(4, 2, 0, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 10);
        }

        @Test
        @DisplayName("KDA in [1.0, 2.0) → 0")
        void kdaAverage() {
            // KDA = 3/3 = 1.0
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100);
        }

        @Test
        @DisplayName("KDA in [0.5, 1.0) → -20")
        void kdaBelowAvg() {
            // KDA = 1/2 = 0.5
            var stats = dto(1, 2, 0, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 20);
        }

        @Test
        @DisplayName("KDA < 0.5 → -35")
        void kdaPoor() {
            // KDA = 0/5 = 0
            var stats = dto(0, 5, 0, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 35);
        }

        @Test
        @DisplayName("deaths=0이면 deaths=1로 처리 (0 나누기 방지)")
        void deathsZero() {
            // deaths=0 → max(0,1)=1 → KDA=5/1=5.0 → +50
            var stats = dto(5, 0, 0, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 50);
        }
    }

    @Nested
    @DisplayName("라운드 승률 보너스/패널티 (KDA·HSR 중립)")
    class RoundWinRateTest {

        @Test
        @DisplayName("라운드 승률 >= 0.6 → +15")
        void highRoundWinRate() {
            // 16/25 = 0.64
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, 16, 25, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 15);
        }

        @Test
        @DisplayName("라운드 승률 in [0.4, 0.6) → 0")
        void avgRoundWinRate() {
            // 10/25 = 0.40
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100);
        }

        @Test
        @DisplayName("라운드 승률 < 0.4 → -10")
        void lowRoundWinRate() {
            // 3/20 = 0.15
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, 3, 20, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 10);
        }

        @Test
        @DisplayName("roundsPlayed=0이면 0 (0 나누기 방지)")
        void roundsPlayedZero() {
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, 0, 0, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("헤드샷율 보너스/패널티 (KDA·RWR 중립)")
    class HeadshotRateTest {

        @Test
        @DisplayName("헤드샷율 >= 0.3 → +20")
        void highHsr() {
            // 30/100 = 0.30
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, 30, 100);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 20);
        }

        @Test
        @DisplayName("헤드샷율 >= 0.2 → +10")
        void goodHsr() {
            // 20/100 = 0.20
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, 20, 100);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 10);
        }

        @Test
        @DisplayName("헤드샷율 >= 0.1 → 0")
        void avgHsr() {
            // 10/100 = 0.10 (중립값과 동일)
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(100);
        }

        @Test
        @DisplayName("헤드샷율 < 0.1 → -5")
        void lowHsr() {
            // 5/100 = 0.05
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, 5, 100);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 5);
        }

        @Test
        @DisplayName("totalShots=0이면 0 (0 나누기 방지)")
        void totalShotsZero() {
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, 0, 0);
            assertThat(engine.calculate(stats)).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("승리 보너스")
    class WinBonusTest {

        @Test
        @DisplayName("승리 시 패배 대비 +10")
        void winAddsTen() {
            var won = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.VICTORY, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            var lost = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.DEFEAT, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(won) - engine.calculate(lost)).isEqualTo(10);
        }

        @Test
        @DisplayName("승리 → 기준 점수 110")
        void wonScore() {
            var stats = dto(N_KILLS, N_DEATHS, N_ASSISTS, MatchResult.VICTORY, N_RWR_WON, N_RWR_PLAYED, N_HS, N_SHOTS);
            assertThat(engine.calculate(stats)).isEqualTo(110);
        }
    }

    @Nested
    @DisplayName("극단값")
    class EdgeCaseTest {

        @Test
        @DisplayName("모든 스탯 0 → KDA 패널티만 적용 (RWR·HSR guard 동작)")
        void allZeroStats() {
            // deaths=0→1, KDA=0<0.5→-35. roundsPlayed=0→guard→0. totalShots=0→guard→0
            var stats = dto(0, 0, 0, MatchResult.DEFEAT, 0, 0, 0, 0);
            assertThat(engine.calculate(stats)).isEqualTo(100 - 35);
        }

        @Test
        @DisplayName("퍼펙트 게임 → 모든 보너스 합산")
        void perfectGame() {
            // KDA=(30+10)/1=40≥5→+50, RWR=13/13=1.0≥0.6→+15, HSR=50/100=0.5≥0.3→+20, win→+10
            var stats = dto(30, 0, 10, MatchResult.VICTORY, 13, 13, 50, 100);
            assertThat(engine.calculate(stats)).isEqualTo(100 + 50 + 15 + 20 + 10);
        }
    }
}
