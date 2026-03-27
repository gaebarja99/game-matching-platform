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

import java.nio.file.Files;
import java.nio.file.Paths;
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
        @DisplayName("null 이면 빈 문자열 반환")
        void format_null_returnsEmpty() {
            assertThat(formatter.format(null)).isEmpty();
        }

        @Test
        @DisplayName("최소 DTO로 플레이어 정보와 기본 블록만 출력")
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
        @DisplayName("matchStats 가 있으면 핵심 지표를 모두 출력")
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
            assertThat(result).contains("[매치 평균 기여도 점수]: 105");
        }

        @Test
        @DisplayName("라운드 스탯이 있으면 예시 라운드를 출력")
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
        @DisplayName("maxRoundExamples 가 0 이면 라운드 예시는 제외")
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
            assertThat(result).doesNotContain("[라운드통계 예시");
            assertThat(result).doesNotContain("R0:");
        }

        @Test
        @DisplayName("displayName 과 agent 가 null 이면 fallback 출력")
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
        @DisplayName("null 이면 빈 문자열 반환")
        void formatAll_null_returnsEmpty() {
            assertThat(formatter.formatAll(null)).isEmpty();
        }

        @Test
        @DisplayName("빈 리스트면 빈 문자열 반환")
        void formatAll_emptyList_returnsEmpty() {
            assertThat(formatter.formatAll(List.of())).isEmpty();
        }

        @Test
        @DisplayName("여러 플레이어는 줄바꿈으로 연결")
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
    @DisplayName("formatSummary - LLM 요약")
    class FormatSummaryTest {

        @Test
        @DisplayName("null 이면 빈 문자열 반환")
        void formatSummary_null_returnsEmpty() {
            assertThat(formatter.formatSummary(null)).isEmpty();
        }

        @Test
        @DisplayName("matchStats 만 있으면 매치 요약 블록 출력")
        void formatSummary_matchStatsOnly_outputsMatchBlock() {
            var matchStats = ValorantMatchStatsDTO.builder()
                    .game("VALORANT")
                    .kills(8)
                    .deaths(17)
                    .assists(4)
                    .result(MatchResult.DEFEAT)
                    .roundsPlayed(23)
                    .roundsWon(10)
                    .kd(0.47)
                    .adr(81.0)
                    .kast(69.6)
                    .headShotRate(22.2)
                    .avgDamageDifference(-45.9)
                    .matchAverageContributionScore(102)
                    .firstBloods(1)
                    .firstDeaths(4)
                    .multiKill(1)
                    .doubleKill(1)
                    .build();

            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("theDoctorr#0000")
                    .playerTeam("Red")
                    .agent("Chamber")
                    .matchStats(matchStats)
                    .roundStats(new ArrayList<>())
                    .build();

            String result = formatter.formatSummary(dto);

            assertThat(result).contains("[매치 요약]");
            assertThat(result).contains("플레이어: theDoctorr#0000 | 팀: Red | 에이전트: Chamber");
            assertThat(result).contains("결과: 패배");
            assertThat(result).contains("스코어: 10-13 (23라운드)");
            assertThat(result).contains("승리기여도 점수: 102");
            assertThat(result).contains("KDA: 8/17/4");
            assertThat(result).contains("KD 0.47");
            assertThat(result).contains("KAST: 70%");
            assertThat(result).contains("ADR: 81");
            assertThat(result).contains("평균피해격차: -46");
            assertThat(result).contains("헤드샷율: 22%");
            assertThat(result).contains("first kill: 1회");
            assertThat(result).contains("First Death: 4회");
            assertThat(result).contains("멀티킬: 1 (더블킬 1회)");
            assertThat(result).doesNotContain("[라운드별]");
        }

        @Test
        @DisplayName("라운드 스탯이 있으면 라운드별 블록과 라인 출력")
        void formatSummary_withRoundStats_outputsRoundLines() {
            var round1 = ValorantRoundStatsDTO.builder()
                    .roundIndex(0)
                    .roundWon(false)
                    .died(true)
                    .kills(1)
                    .damage(159)
                    .roundContributionScore(97)
                    .FirstKill(false)
                    .FirstDeath(false)
                    .build();
            var round2 = ValorantRoundStatsDTO.builder()
                    .roundIndex(1)
                    .roundWon(false)
                    .died(true)
                    .kills(0)
                    .damage(0)
                    .roundContributionScore(90)
                    .FirstKill(false)
                    .FirstDeath(false)
                    .build();

            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("P#t")
                    .matchStats(ValorantMatchStatsDTO.builder()
                            .game("VALORANT")
                            .kills(1)
                            .deaths(1)
                            .assists(0)
                            .result(MatchResult.DEFEAT)
                            .roundsPlayed(2)
                            .roundsWon(0)
                            .matchAverageContributionScore(93)
                            .build())
                    .roundStats(List.of(round1, round2))
                    .build();

            String result = formatter.formatSummary(dto);

            assertThat(result).contains("[라운드별]");
            assertThat(result).contains("R0: 패배 킬1 데스1 딜159 승리기여도97");
            assertThat(result).contains("R1: 패배 킬0 데스1 딜0 승리기여도90");
        }

        @Test
        @DisplayName("maxRoundLines 가 0 이면 라운드 블록 제외")
        void formatSummary_maxRoundLinesZero_excludesRounds() {
            var round = ValorantRoundStatsDTO.builder()
                    .roundIndex(0)
                    .roundWon(true)
                    .roundContributionScore(110)
                    .build();
            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("P#t")
                    .matchStats(ValorantMatchStatsDTO.builder()
                            .game("VALORANT")
                            .roundsPlayed(1)
                            .roundsWon(1)
                            .matchAverageContributionScore(110)
                            .build())
                    .roundStats(List.of(round))
                    .build();

            String result = formatter.formatSummary(dto, 0);

            assertThat(result).contains("[매치 요약]");
            assertThat(result).doesNotContain("[라운드별]");
            assertThat(result).doesNotContain("R0:");
        }

        @Test
        @DisplayName("maxRoundLines 가 1 이면 첫 라운드만 출력")
        void formatSummary_maxRoundLinesOne_outputsFirstRoundOnly() {
            var r0 = ValorantRoundStatsDTO.builder().roundIndex(0).roundContributionScore(100).build();
            var r1 = ValorantRoundStatsDTO.builder().roundIndex(1).roundContributionScore(90).build();
            var dto = ValorantPlayerMatchStatsDTO.builder()
                    .playerDisplayName("P#t")
                    .matchStats(ValorantMatchStatsDTO.builder().game("VALORANT").roundsPlayed(2).build())
                    .roundStats(List.of(r0, r1))
                    .build();

            String result = formatter.formatSummary(dto, 1);

            assertThat(result).contains("R0:");
            assertThat(result).doesNotContain("R1:");
        }
    }

    @Nested
    @DisplayName("integration - sample data")
    class IntegrationTest {

        @Test
        @DisplayName("샘플 JSON 기반 format 결과에 핵심 필드 포함")
        void format_sampleData_containsExpectedFields() throws Exception {
            var jsonService = new ValorantMatchJsonService();
            var matchMapper = new ValorantMatchMapper();
            var roundStatsMapper = new ValorantRoundStatsMapper();
            var scoreService = new ValorantScoreService(
                    new KillContextExtractor(),
                    new ValorantRoundScoreEngine(),
                    new RoundScoreInputBuilder()
            );
            var statsMapper = new ValorantMatchStatsMapper(roundStatsMapper, scoreService);

            String json = Files.readString(
                    Paths.get("src/test/resources/samples/valorant/valorant_match_sample.json"));
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
