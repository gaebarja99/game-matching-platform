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
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.service.ai.LlmEvaluationService;
import com.gamematcher.service.ai.LlmEvaluationServiceImpl;
import com.gamematcher.service.ai.ValorantEvaluationPromptBuilder;
import com.gamematcher.service.ai.ValorantLlmEvaluationService;
import com.gamematcher.service.ai.score.ValorantRoundScoreEngine;
import com.gamematcher.service.ai.score.ValorantScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

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
    private ValorantEvaluationPromptBuilder promptBuilder;

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
        promptBuilder = new ValorantEvaluationPromptBuilder(promptFormatter);
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

    @Test
    @DisplayName("한 명 플레이어 스탯 LLM용 요약 출력")
    void outputSinglePlayerMatchStatsSummary() throws Exception {
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

        String summary = promptFormatter.formatSummary(single);
        String textContent = "=== LLM용 요약 (formatSummary) ===\n\n" + summary;

        Path txtPath = outDir.resolve("valorant_single_player_match_stats_summary.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("요약 출력: " + txtPath.toAbsolutePath());

        assertThat(summary).contains("[매치 요약]");
        assertThat(summary).contains("[라운드별]");
    }

    @Test
    @DisplayName("한 명 플레이어 LLM용 전체 프롬프트 출력 (역할+데이터+출력형식)")
    void outputSinglePlayerFullPrompt() throws Exception {
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

        String fullPrompt = promptBuilder.build(single);
        String textContent = "=== LLM 전송용 전체 프롬프트 ===\n\n" + fullPrompt;

        Path txtPath = outDir.resolve("valorant_single_player_full_prompt.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("전체 프롬프트 출력: " + txtPath.toAbsolutePath());

        assertThat(fullPrompt).contains("당신은 발로란트 전문");
        assertThat(fullPrompt).contains("[매치 요약]");
        assertThat(fullPrompt).contains("{\"summary\":");
        assertThat(fullPrompt).contains("한국어로 작성하십시오");
    }

    @Test
    @DisplayName("LLM API 응답을 텍스트 파일로 저장 (AI_API_KEY 환경변수 필요)")
    void outputLlmApiResponse() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        // API 키: 환경변수 우선, 없으면 application.properties 기본값 사용
        String apiKey = resolveApiKeyForTest();
        ObjectMapper om = new ObjectMapper();
        LlmEvaluationService llmService = new LlmEvaluationServiceImpl(
                om,
                apiKey != null ? apiKey : "",
                "gpt-5-mini",
                30,
                2
        );
        ValorantLlmEvaluationService valorantLlmService = new ValorantLlmEvaluationService(
                promptBuilder,
                llmService
        );

        // 샘플 데이터 로드
        String json = Files.readString(Paths.get(SAMPLE_PATH));
        var matchDto = jsonService.parseFirstMatch(json);
        assertThat(matchDto).isNotNull();

        var entity = matchMapper.toEntity(matchDto);
        assertThat(entity).isNotNull();

        List<ValorantPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(entity);
        assertThat(playerStats).isNotEmpty();

        int idx = Math.min(SINGLE_PLAYER_INDEX, playerStats.size() - 1);
        ValorantPlayerMatchStatsDTO single = playerStats.get(idx);

        // LLM 평가 호출
        var result = valorantLlmService.evaluate(single);

        // 결과를 텍스트 파일로 저장
        StringBuilder content = new StringBuilder();
        content.append("=== LLM API 평가 결과 ===\n");
        content.append("플레이어: ").append(single.getPlayerDisplayName()).append(" | 에이전트: ").append(single.getAgent()).append("\n\n");

        if (result.isPresent()) {
            LlmEvaluationResponseDTO dto = result.get();
            content.append("[요약]\n").append(dto.getSummary()).append("\n\n");
            String comment = dto.getDetailedComment();
            content.append("[상세 코멘트]\n").append(comment != null ? comment.replace("\\n", "\n") : "").append("\n");
        } else {
            content.append("AI_API_KEY 또는 OPENAI_API_KEY가 설정되지 않았거나 API 호출에 실패했습니다.\n");
            content.append("환경변수를 설정하고 테스트를 다시 실행하세요.\n");
        }

        Path txtPath = outDir.resolve("valorant_llm_api_response.txt");
        Files.writeString(txtPath, content);
        System.out.println("LLM API 응답 출력: " + txtPath.toAbsolutePath());
    }

    @Test
    @DisplayName("LLM API 응답 (gpt-5.2 고급 모델)을 텍스트 파일로 저장 (AI_API_KEY 또는 OPENAI_API_KEY 환경변수 필요)")
    void outputLlmApiResponseWithAdvancedModel() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String apiKey = resolveApiKeyForTest();
        ObjectMapper om = new ObjectMapper();
        LlmEvaluationService llmService = new LlmEvaluationServiceImpl(
                om,
                apiKey != null ? apiKey : "",
                "gpt-5.2",
                45,
                2
        );
        ValorantLlmEvaluationService valorantLlmService = new ValorantLlmEvaluationService(
                promptBuilder,
                llmService
        );

        String json = Files.readString(Paths.get(SAMPLE_PATH));
        var matchDto = jsonService.parseFirstMatch(json);
        assertThat(matchDto).isNotNull();

        var entity = matchMapper.toEntity(matchDto);
        assertThat(entity).isNotNull();

        List<ValorantPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(entity);
        assertThat(playerStats).isNotEmpty();

        int idx = Math.min(SINGLE_PLAYER_INDEX, playerStats.size() - 1);
        ValorantPlayerMatchStatsDTO single = playerStats.get(idx);

        var result = valorantLlmService.evaluate(single);

        StringBuilder content = new StringBuilder();
        content.append("=== LLM API 평가 결과 (gpt-5.2) ===\n");
        content.append("플레이어: ").append(single.getPlayerDisplayName()).append(" | 에이전트: ").append(single.getAgent()).append("\n\n");

        if (result.isPresent()) {
            LlmEvaluationResponseDTO dto = result.get();
            content.append("[요약]\n").append(dto.getSummary()).append("\n\n");
            String comment = dto.getDetailedComment();
            content.append("[상세 코멘트]\n").append(comment != null ? comment.replace("\\n", "\n") : "").append("\n");
        } else {
            content.append("AI_API_KEY 또는 OPENAI_API_KEY가 설정되지 않았거나 API 호출에 실패했습니다.\n");
            content.append("환경변수를 설정하고 테스트를 다시 실행하세요.\n");
        }

        Path txtPath = outDir.resolve("valorant_llm_api_response_gpt52.txt");
        Files.writeString(txtPath, content);
        System.out.println("LLM API 응답 출력 (gpt-5.2): " + txtPath.toAbsolutePath());
    }

    /**
     * 테스트용 API 키: 환경변수 AI_API_KEY, OPENAI_API_KEY 순으로 확인.
     */
    private static String resolveApiKeyForTest() {
        String key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        try {
            Path propsPath = Paths.get("src/main/resources/application.properties");
            if (Files.exists(propsPath)) {
                String content = Files.readString(propsPath);
                var m = Pattern.compile("ai\\.llm\\.api-key=\\$\\{.*?:([^}]*)\\}").matcher(content);
                if (m.find() && m.group(1) != null && !m.group(1).isBlank()) {
                    return m.group(1).trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
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
