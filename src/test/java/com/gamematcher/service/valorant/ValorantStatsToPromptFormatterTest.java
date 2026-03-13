package com.gamematcher.service.valorant;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.ValorantMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import com.gamematcher.mapper.ValorantMatchMapper;
import com.gamematcher.mapper.ValorantMatchStatsMapper;
import com.gamematcher.mapper.ValorantRoundStatsMapper;
import com.gamematcher.service.ai.score.KillContextExtractor;
import com.gamematcher.service.ai.score.RoundScoreInputBuilder;
import com.gamematcher.service.ai.score.ValorantRoundScoreEngine;
import com.gamematcher.service.ai.score.ValorantScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValorantStatsToPromptFormatter 테스트")
class ValorantStatsToPromptFormatterTest {

    private ValorantStatsToPromptFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ValorantStatsToPromptFormatter();
    }

    @Nested
    @DisplayName("format - 단일 플레이어")
    class FormatSingleTest {

        @Test
        @DisplayName("null이 주어지면 빈 문자열 반환")
        void format_null_returnsEmpty() {
            assertThat(formatter.format(null)).isEmpty();
        }

        @Test
        @DisplayName("최소 DTO로 플레이어 정보와 기본 라벨만 출력")
        void format_minimalDto_outputsPlayerInfo() {
            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("Test#1234")
                    .playerPuuid("uuid-abc")
                    .playerTeam("Red")
                    .agent("Jett")
                    .matchId("match-1")
                    .roundStats(new ArrayList<>())
                    .build();

            String result = formatter.format(dto);

            assertThat(result).contains("--- 플레이어: Test#1234 (uuid-abc)");
            assertThat(result).contains("매치ID: match-1");
            assertThat(result).contains("팀: Red");
            assertThat(result).contains("에이전트: Jett");
            assertThat(result).contains("[매치 스탯] (없음)");
            assertThat(result).contains("[라운드 수]: 0");
        }

        @Test
        @DisplayName("matchStats가 있으면 K/D/A, ADR, KAST, 멀티킬 등 출력")
        void format_withMatchStats_outputsAllIndicators() {
            var matchStats = ValorantMatchStatsDTO.builder()
                    .game("VALORANT")
                    .kills(10)
                    .deaths(5)
                    .assists(3)
                    .result(MatchResult.VICTORY)
                    .roundsPlayed(13)
                    .roundsWon(7)
                    .adr(150.5)
                    .kast(76.9)
                    .headShotRate(25.0)
                    .firstBloods(2)
                    .firstDeaths(1)
                    .multiKill(2)
                    .doubleKill(2)
                    .tripleKill(0)
                    .quadraKill(0)
                    .pentaKill(0)
                    .overKill(0)
                    .avgDamageDifference(45.2)
                    .matchAverageContributionScore(105)
                    .build();

            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("Player#tag")
                    .playerPuuid("puuid-1")
                    .matchStats(matchStats)
                    .roundStats(new ArrayList<>())
                    .build();

            String result = formatter.format(dto);

            assertThat(result).contains("K/D/A: 10/5/3");
            assertThat(result).contains("Rounds: 7/13");
            assertThat(result).contains("ADR: 150.5");
            assertThat(result).contains("KAST: 76.9%");
            assertThat(result).contains("헤드샷률: 25.0%");
            assertThat(result).contains("FirstBlood: 2");
            assertThat(result).contains("FirstDeath: 1");
            assertThat(result).contains("멀티킬: 2");
            assertThat(result).contains("2K:2");
            assertThat(result).contains("DDΔ: 45.2");
            assertThat(result).contains("매치 평균 기여도 점수]: 105");
        }

        @Test
        @DisplayName("라운드 스탯이 있으면 예시 라운드 출력 (기본 2개)")
        void format_withRoundStats_outputsRoundExamples() {
            var round1 = ValorantRoundStatsDTO.builder()
                    .roundIndex(0)
                    .damage(150)
                    .kills(1)
                    .roundWon(true)
                    .died(false)
                    .FirstKill(true)
                    .FirstDeath(false)
                    .roundContributionScore(110)
                    .build();
            var round2 = ValorantRoundStatsDTO.builder()
                    .roundIndex(1)
                    .damage(80)
                    .kills(0)
                    .roundWon(false)
                    .died(true)
                    .FirstKill(false)
                    .FirstDeath(true)
                    .roundContributionScore(90)
                    .build();

            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("P#t")
                    .roundStats(List.of(round1, round2))
                    .build();

            String result = formatter.format(dto);

            assertThat(result).contains("[라운드별 예시 - 1·2라운드]");
            assertThat(result).contains("R0: damage=150");
            assertThat(result).contains("kills=1");
            assertThat(result).contains("roundWon=true");
            assertThat(result).contains("died=false");
            assertThat(result).contains("FK=true");
            assertThat(result).contains("FD=false");
            assertThat(result).contains("roundContributionScore=110");
            assertThat(result).contains("R1: damage=80");
            assertThat(result).contains("[라운드 기여도 평균]");
        }

        @Test
        @DisplayName("maxRoundExamples=0이면 라운드 예시 제외")
        void format_maxRoundExamplesZero_excludesRoundExamples() {
            var round = ValorantRoundStatsDTO.builder()
                    .roundIndex(0)
                    .damage(100)
                    .roundContributionScore(100)
                    .build();
            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("P#t")
                    .roundStats(List.of(round))
                    .build();

            String result = formatter.format(dto, 0);

            assertThat(result).contains("[라운드 수]: 1");
            assertThat(result).doesNotContain("[라운드별 예시");
            assertThat(result).doesNotContain("R0:");
        }

        @Test
        @DisplayName("displayName·agent null 시 기본값 출력")
        void format_nullDisplayName_outputsFallback() {
            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerPuuid("puuid")
                    .roundStats(new ArrayList<>())
                    .build();

            String result = formatter.format(dto);

            assertThat(result).contains("(이름없음)");
            assertThat(result).contains("에이전트: -");
        }
    }

    @Nested
    @DisplayName("formatAll - 전체 플레이어")
    class FormatAllTest {

        @Test
        @DisplayName("null이 주어지면 빈 문자열 반환")
        void formatAll_null_returnsEmpty() {
            assertThat(formatter.formatAll(null)).isEmpty();
        }

        @Test
        @DisplayName("빈 리스트면 빈 문자열 반환")
        void formatAll_emptyList_returnsEmpty() {
            assertThat(formatter.formatAll(List.of())).isEmpty();
        }

        @Test
        @DisplayName("여러 플레이어 시 각각 포맷 후 줄바꿈으로 연결")
        void formatAll_multiplePlayers_joinsWithNewline() {
            var p1 = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("P1#t1")
                    .roundStats(new ArrayList<>())
                    .build();
            var p2 = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("P2#t2")
                    .roundStats(new ArrayList<>())
                    .build();

            String result = formatter.formatAll(List.of(p1, p2));

            assertThat(result).contains("--- 플레이어: P1#t1");
            assertThat(result).contains("--- 플레이어: P2#t2");
            assertThat(result).contains("\n--- 플레이어: P2");
        }
    }

    @Nested
    @DisplayName("통합 - 샘플 데이터")
    class IntegrationTest {

        @Test
        @DisplayName("샘플 JSON → DTO → format 결과에 필수 필드 포함")
        void format_sampleData_containsExpectedFields() throws Exception {
            var jsonService = new ValorantMatchJsonService();
            var matchMapper = new ValorantMatchMapper();
            var roundStatsMapper = new ValorantRoundStatsMapper();
            var scoreService = new com.gamematcher.service.ai.score.ValorantScoreService(
                    new com.gamematcher.service.ai.score.KillContextExtractor(),
                    new com.gamematcher.service.ai.score.ValorantRoundScoreEngine(),
                    new com.gamematcher.service.ai.score.RoundScoreInputBuilder()
            );
            var statsMapper = new ValorantMatchStatsMapper(roundStatsMapper, scoreService);

            String json = java.nio.file.Files.readString(
                    java.nio.file.Paths.get("src/test/resources/samples/valorant/valorant_match_sample.json"));
            var matchDto = jsonService.parseFirstMatch(json);
            var entity = matchMapper.toEntity(matchDto);
            var playerStats = statsMapper.toPlayerMatchStatsDtos(entity);

            assertThat(playerStats).isNotEmpty();
            ValorantPlayerMatchStatsDTO first = playerStats.get(0);

            String result = formatter.format(first);

            assertThat(result).contains("--- 플레이어:");
            assertThat(result).contains("[매치 스탯]");
            assertThat(result).contains("K/D/A:");
            assertThat(result).contains("Rounds:");
            assertThat(result).contains("ADR:");
            assertThat(result).contains("KAST:");
            assertThat(result).contains("[라운드 수]:");
        }
    }
}
