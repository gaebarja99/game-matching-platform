package com.gamematcher.service.lol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.LolPlayerMatchStatsDTO;
import com.gamematcher.mapper.LolMatchStatsMapper;
import com.gamematcher.service.ai.LlmEvaluationService;
import com.gamematcher.service.ai.LlmEvaluationServiceImpl;
import com.gamematcher.service.ai.LolEvaluationPromptBuilder;
import com.gamematcher.service.ai.LolLlmEvaluationService;
import com.gamematcher.testsupport.LlmTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 샘플 데이터로 LolPlayerMatchStatsDTO를 생성하고,
 * 결과를 텍스트·JSON 파일로 출력하여 눈으로 확인하는 테스트.
 *
 * 출력 위치: target/lol-stats-output/
 */
@DisplayName("LolPlayerMatchStats 수동 확인용 출력 테스트")
class LolPlayerMatchStatsOutputTest {

    private static final String MATCH_SAMPLE_PATH = "src/test/resources/samples/lol/lol_match_sample.json";
    private static final String TIMELINE_SAMPLE_PATH = "src/test/resources/samples/lol/lol_timeline_sample.json";
    private static final String OUTPUT_DIR = "target/lol-stats-output";

    /** 한 명 출력 시 사용할 플레이어 인덱스 (0 = 첫 번째 플레이어) */
    private static final int SINGLE_PLAYER_INDEX = 0;

    private LolMatchJsonService jsonService;
    private LolMatchStatsMapper statsMapper;
    private LolStatsToPromptFormatter formatter;
    private LolEvaluationPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        jsonService = new LolMatchJsonService();
        statsMapper = new LolMatchStatsMapper();
        formatter = new LolStatsToPromptFormatter();
        promptBuilder = new LolEvaluationPromptBuilder(formatter);
    }

    @Test
    @DisplayName("샘플(Match만) → LolPlayerMatchStatsDTO → 텍스트·JSON 파일 출력")
    void outputPlayerMatchStats() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        var matchDto = jsonService.parseMatchDetail(matchJson);
        assertThat(matchDto).isNotNull();

        List<LolPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(matchDto);
        assertThat(playerStats).isNotEmpty();

        ObjectMapper om = createOutputObjectMapper();
        Path jsonPath = outDir.resolve("lol_player_match_stats.json");
        om.writeValue(jsonPath.toFile(), playerStats);
        System.out.println("JSON 출력: " + jsonPath.toAbsolutePath());

        String textContent = "=== LolPlayerMatchStatsDTO 출력 (Match만) ===\n\n"
                + formatter.formatAll(playerStats);

        Path txtPath = outDir.resolve("lol_player_match_stats.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("텍스트 출력: " + txtPath.toAbsolutePath());
    }

    @Test
    @DisplayName("샘플(Match+Timeline) → LolPlayerMatchStatsDTO → 텍스트·JSON 파일 출력")
    void outputPlayerMatchStatsWithTimeline() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        String timelineJson = Files.readString(Paths.get(TIMELINE_SAMPLE_PATH));
        var matchDto = jsonService.parseMatchDetail(matchJson);
        var timelineDto = jsonService.parseTimelineDetail(timelineJson);
        assertThat(matchDto).isNotNull();
        assertThat(timelineDto).isNotNull();

        List<LolPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(matchDto, timelineDto);
        assertThat(playerStats).isNotEmpty();

        ObjectMapper om = createOutputObjectMapper();
        Path jsonPath = outDir.resolve("lol_player_match_stats_with_timeline.json");
        om.writeValue(jsonPath.toFile(), playerStats);
        System.out.println("JSON 출력 (Match+Timeline): " + jsonPath.toAbsolutePath());

        String textContent = "=== LolPlayerMatchStatsDTO 출력 (Match+Timeline) ===\n\n"
                + formatter.formatAll(playerStats);

        Path txtPath = outDir.resolve("lol_player_match_stats_with_timeline.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("텍스트 출력 (Match+Timeline): " + txtPath.toAbsolutePath());
    }

    @Test
    @DisplayName("한 명 플레이어 스탯만 JSON·텍스트 출력")
    void outputSinglePlayerMatchStats() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        var matchDto = jsonService.parseMatchDetail(matchJson);
        assertThat(matchDto).isNotNull();

        List<LolPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(matchDto);
        assertThat(playerStats).isNotEmpty();

        int idx = Math.min(SINGLE_PLAYER_INDEX, playerStats.size() - 1);
        LolPlayerMatchStatsDTO single = playerStats.get(idx);

        ObjectMapper om = createOutputObjectMapper();
        Path jsonPath = outDir.resolve("lol_single_player_match_stats.json");
        om.writeValue(jsonPath.toFile(), single);
        System.out.println("JSON 출력 (1인): " + jsonPath.toAbsolutePath());

        String textContent = "=== LolPlayerMatchStatsDTO (1인) ===\n\n"
                + formatter.format(single);

        Path txtPath = outDir.resolve("lol_single_player_match_stats.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("텍스트 출력 (1인): " + txtPath.toAbsolutePath());
    }

    @Test
    @DisplayName("한 명 플레이어 스탯 요약 출력 (formatSummary)")
    void outputSinglePlayerMatchStatsSummary() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        String timelineJson = Files.readString(Paths.get(TIMELINE_SAMPLE_PATH));
        var matchDto = jsonService.parseMatchDetail(matchJson);
        var timelineDto = jsonService.parseTimelineDetail(timelineJson);
        assertThat(matchDto).isNotNull();

        List<LolPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(matchDto, timelineDto);
        assertThat(playerStats).isNotEmpty();

        int idx = Math.min(SINGLE_PLAYER_INDEX, playerStats.size() - 1);
        LolPlayerMatchStatsDTO single = playerStats.get(idx);

        String summary = formatter.formatSummary(single);
        String textContent = "=== LLM용 요약 (formatSummary) ===\n\n" + summary;

        Path txtPath = outDir.resolve("lol_single_player_match_stats_summary.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("요약 출력: " + txtPath.toAbsolutePath());

        assertThat(summary).contains("[매치 요약]");
    }

    @Test
    @DisplayName("LLM 전송용 전체 프롬프트 출력")
    void outputFullPromptForSinglePlayer() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        String timelineJson = Files.readString(Paths.get(TIMELINE_SAMPLE_PATH));
        var matchDto = jsonService.parseMatchDetail(matchJson);
        var timelineDto = jsonService.parseTimelineDetail(timelineJson);
        assertThat(matchDto).isNotNull();
        assertThat(timelineDto).isNotNull();

        List<LolPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(matchDto, timelineDto);
        assertThat(playerStats).isNotEmpty();

        int idx = Math.min(SINGLE_PLAYER_INDEX, playerStats.size() - 1);
        LolPlayerMatchStatsDTO single = playerStats.get(idx);

        String fullPrompt = promptBuilder.build(single);
        String textContent = "=== LLM 전송용 전체 프롬프트 ===\n\n" + fullPrompt;

        Path txtPath = outDir.resolve("lol_single_player_full_prompt.txt");
        Files.writeString(txtPath, textContent);
        System.out.println("전체 프롬프트 출력: " + txtPath.toAbsolutePath());

        assertThat(fullPrompt).contains("당신은 리그 오브 레전드 전문");
        assertThat(fullPrompt).contains("[매치 요약]");
        assertThat(fullPrompt).contains("{\"summary\":");
        assertThat(fullPrompt).contains("한국어로 작성하십시오");
    }

    @Test
    @DisplayName("LLM API 응답을 텍스트 파일로 저장 (AI_API_KEY 또는 OPENAI_API_KEY 환경변수 필요)")
    void outputLlmApiResponse() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String apiKey = LlmTestSupport.resolveApiKeyForTest();
        ObjectMapper om = new ObjectMapper();
        String model = LlmTestSupport.resolveLlmModelForTest("gpt-5-mini");
        int timeoutSec = LlmTestSupport.resolveLlmTimeoutSecondsForTest(30);
        int maxRetries = LlmTestSupport.resolveLlmMaxRetriesForTest(2);
        LlmEvaluationService llmService = new LlmEvaluationServiceImpl(
                om,
                apiKey != null ? apiKey : "",
                model,
                timeoutSec,
                maxRetries
        );
        LolLlmEvaluationService lolLlmService = new LolLlmEvaluationService(
                promptBuilder,
                llmService
        );

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        String timelineJson = Files.readString(Paths.get(TIMELINE_SAMPLE_PATH));
        var matchDto = jsonService.parseMatchDetail(matchJson);
        var timelineDto = jsonService.parseTimelineDetail(timelineJson);
        assertThat(matchDto).isNotNull();

        List<LolPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(matchDto, timelineDto);
        assertThat(playerStats).isNotEmpty();

        int idx = Math.min(SINGLE_PLAYER_INDEX, playerStats.size() - 1);
        LolPlayerMatchStatsDTO single = playerStats.get(idx);

        var result = lolLlmService.evaluate(single);

        StringBuilder content = new StringBuilder();
        content.append("=== LLM API 평가 결과 (LoL) ===\n");
        content.append("플레이어: ").append(single.getPlayerDisplayName())
                .append(" | 챔피언: ").append(single.getChampion())
                .append(" | 포지션: ").append(single.getTeamPosition()).append("\n\n");

        if (result.isPresent()) {
            LlmEvaluationResponseDTO dto = result.get();
            content.append("[요약]\n").append(dto.getSummary()).append("\n\n");
            String comment = dto.getDetailedComment();
            if (comment != null) {
                comment = comment.replace("\\n", "\n");
            }
            content.append("[상세 코멘트]\n").append(comment != null ? comment : "").append("\n");
        } else {
            if (apiKey == null || apiKey.isBlank()) {
                content.append("API 키가 비어 있습니다. 환경변수 AI_API_KEY / OPENAI_API_KEY 또는 application.properties 의 ai.llm.api-key 기본값을 확인하세요.\n");
            } else {
                content.append("API 키는 있으나 모델 호출에 실패했습니다. 모델명·쿼터·네트워크·키 권한을 확인하세요. (현재 모델: ").append(model).append(")\n");
            }
        }

        Path txtPath = outDir.resolve("lol_llm_api_response.txt");
        Files.writeString(txtPath, content);
        System.out.println("LLM API 응답 출력: " + txtPath.toAbsolutePath());
    }

    @Test
    @DisplayName("LLM API 응답 (gpt-5.2 고급 모델)을 텍스트 파일로 저장 (AI_API_KEY 또는 OPENAI_API_KEY 환경변수 필요)")
    void outputLlmApiResponseWithAdvancedModel() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String apiKey = LlmTestSupport.resolveApiKeyForTest();
        ObjectMapper om = new ObjectMapper();
        LlmEvaluationService llmService = new LlmEvaluationServiceImpl(
                om,
                apiKey != null ? apiKey : "",
                "gpt-5.2",
                LlmTestSupport.resolveLlmTimeoutSecondsForTest(45),
                LlmTestSupport.resolveLlmMaxRetriesForTest(2)
        );
        LolLlmEvaluationService lolLlmService = new LolLlmEvaluationService(
                promptBuilder,
                llmService
        );

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        String timelineJson = Files.readString(Paths.get(TIMELINE_SAMPLE_PATH));
        var matchDto = jsonService.parseMatchDetail(matchJson);
        var timelineDto = jsonService.parseTimelineDetail(timelineJson);
        assertThat(matchDto).isNotNull();

        List<LolPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(matchDto, timelineDto);
        assertThat(playerStats).isNotEmpty();

        int idx = Math.min(SINGLE_PLAYER_INDEX, playerStats.size() - 1);
        LolPlayerMatchStatsDTO single = playerStats.get(idx);

        var result = lolLlmService.evaluate(single);

        StringBuilder content = new StringBuilder();
        content.append("=== LLM API 평가 결과 (LoL, gpt-5.2) ===\n");
        content.append("플레이어: ").append(single.getPlayerDisplayName())
                .append(" | 챔피언: ").append(single.getChampion())
                .append(" | 포지션: ").append(single.getTeamPosition()).append("\n\n");

        if (result.isPresent()) {
            LlmEvaluationResponseDTO dto = result.get();
            content.append("[요약]\n").append(dto.getSummary()).append("\n\n");
            String comment = dto.getDetailedComment();
            if (comment != null) {
                comment = comment.replace("\\n", "\n");
            }
            content.append("[상세 코멘트]\n").append(comment != null ? comment : "").append("\n");
        } else {
            if (apiKey == null || apiKey.isBlank()) {
                content.append("API 키가 비어 있습니다. 환경변수 또는 application.properties 의 ai.llm.api-key 를 확인하세요.\n");
            } else {
                content.append("API 키는 있으나 gpt-5.2 호출에 실패했습니다. 계정에서 해당 모델 사용 가능 여부를 확인하세요.\n");
            }
        }

        Path txtPath = outDir.resolve("lol_llm_api_response_gpt52.txt");
        Files.writeString(txtPath, content);
        System.out.println("LLM API 응답 출력 (gpt-5.2): " + txtPath.toAbsolutePath());
    }

    private static ObjectMapper createOutputObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om;
    }
}
