package com.gamematcher.service.valorant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.mapper.ValorantMatchMapper;
import com.gamematcher.mapper.ValorantMatchStatsMapper;
import com.gamematcher.mapper.ValorantRoundStatsMapper;
import com.gamematcher.service.ai.score.KillContextExtractor;
import com.gamematcher.service.ai.score.RoundScoreInputBuilder;
import com.gamematcher.service.ai.score.ValorantRoundScoreEngine;
import com.gamematcher.service.ai.score.ValorantScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 샘플 데이터로 ValorantPlayerMatchStatsDTO를 생성하고,
 * 결과를 텍스트·JSON 파일로 출력하여 눈으로 확인하는 테스트.
 *
 * 출력 위치: target/valorant-stats-output/
 */
@DisplayName("ValorantPlayerMatchStats 수동 확인용 출력 테스트")
class ValorantPlayerMatchStatsOutputTest {

    private static final String SAMPLE_PATH = "src/test/resources/samples/valorant/valorant_match_sample.json";
    private static final String OUTPUT_DIR = "target/valorant-stats-output";

    /** 한 명 출력 시 사용할 플레이어 인덱스 (0 = 첫 번째 플레이어) */
    private static final int SINGLE_PLAYER_INDEX = 0;

    private ValorantMatchJsonService jsonService;
    private ValorantMatchMapper matchMapper;
    private ValorantMatchStatsMapper statsMapper;
    private ValorantStatsToPromptFormatter promptFormatter;

    @BeforeEach
    void setUp() {
        jsonService = new ValorantMatchJsonService();
        matchMapper = new ValorantMatchMapper();
        var roundStatsMapper = new ValorantRoundStatsMapper();
        var scoreService = new ValorantScoreService(
                new KillContextExtractor(),
                new ValorantRoundScoreEngine(),
                new RoundScoreInputBuilder()
        );
        statsMapper = new ValorantMatchStatsMapper(roundStatsMapper, scoreService);
        promptFormatter = new ValorantStatsToPromptFormatter();
    }

    @Test
    @DisplayName("샘플 → ValorantPlayerMatchStatsDTO → 텍스트·JSON 파일 출력")
    void outputPlayerMatchStats() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        // 1. 샘플 JSON → DTO → Entity → ValorantPlayerMatchStatsDTO (roundContributionScore 포함)
        String json = Files.readString(Paths.get(SAMPLE_PATH));
        var matchDto = jsonService.parseFirstMatch(json);
        assertThat(matchDto).isNotNull();

        var entity = matchMapper.toEntity(matchDto);
        assertThat(entity).isNotNull();

        List<ValorantPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(entity);
        assertThat(playerStats).isNotEmpty();

        // 2. JSON 파일 출력 (pretty print, 라운드별 playerPuuid 제외 - 상위에 있음)
        ObjectMapper om = createOutputObjectMapper();
        clearPlayerPuuidFromRoundStats(playerStats);
        Path jsonPath = outDir.resolve("valorant_player_match_stats.json");
        om.writeValue(jsonPath.toFile(), playerStats);
        System.out.println("JSON 출력: " + jsonPath.toAbsolutePath());

        // 3. 텍스트 파일 출력 (프롬프트 포맷터 사용)
        String textContent = "=== ValorantPlayerMatchStatsDTO 출력 ===\n\n"
                + promptFormatter.formatAll(playerStats);

        Path txtPath = outDir.resolve("valorant_player_match_stats.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("텍스트 출력: " + txtPath.toAbsolutePath());
    }

    @Test
    @DisplayName("한 명 플레이어 스탯만 JSON·텍스트 출력")
    void outputSinglePlayerMatchStats() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String json = Files.readString(Paths.get(SAMPLE_PATH));
        var matchDto = jsonService.parseFirstMatch(json);
        assertThat(matchDto).isNotNull();

        var entity = matchMapper.toEntity(matchDto);
        assertThat(entity).isNotNull();

        List<ValorantPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(entity);
        assertThat(playerStats).isNotEmpty();

        int idx = Math.min(SINGLE_PLAYER_INDEX, playerStats.size() - 1);
        ValorantPlayerMatchStatsDTO single = playerStats.get(idx);

        ObjectMapper om = createOutputObjectMapper();
        clearPlayerPuuidFromRoundStats(List.of(single));
        Path jsonPath = outDir.resolve("valorant_single_player_match_stats.json");
        om.writeValue(jsonPath.toFile(), single);
        System.out.println("JSON 출력 (1인): " + jsonPath.toAbsolutePath());

        String textContent = "=== ValorantPlayerMatchStatsDTO (1인) ===\n\n"
                + promptFormatter.format(single);

        Path txtPath = outDir.resolve("valorant_single_player_match_stats.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("텍스트 출력 (1인): " + txtPath.toAbsolutePath());
    }

    private static ObjectMapper createOutputObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om;
    }

    /** 라운드별 playerPuuid 제거 (상위 플레이어에 이미 있음) */
    private static void clearPlayerPuuidFromRoundStats(List<ValorantPlayerMatchStatsDTO> playerStats) {
        for (ValorantPlayerMatchStatsDTO ps : playerStats) {
            if (ps.getRoundStats() != null) {
                ps.getRoundStats().forEach(rs -> rs.setPlayerPuuid(null));
            }
        }
    }
}
