package com.gamematcher.service.pubg;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.PubgPlayerMatchStatsDTO;
import com.gamematcher.dto.pubg.PubgMatchApiResponse;
import com.gamematcher.dto.pubg.PubgTelemetryEventRowDto;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import com.gamematcher.entity.match.pubg.PubgTelemetryEvent;
import com.gamematcher.mapper.PubgMatchMapper;
import com.gamematcher.service.ai.LlmEvaluationService;
import com.gamematcher.service.ai.LlmEvaluationServiceImpl;
import com.gamematcher.service.ai.PubgEvaluationPromptBuilder;
import com.gamematcher.service.ai.PubgLlmEvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PUBG 샘플 데이터로 PubgPlayerMatchStatsDTO/프롬프트를 생성하고,
 * (선택) 실제 LLM API 응답을 텍스트 파일로 출력하는 수동 확인 테스트.
 *
 * 출력 위치: target/pubg-stats-output/
 *
 * <p>LLM 호출은 환경변수 {@code AI_API_KEY} 또는 {@code OPENAI_API_KEY}가 설정된 경우에만 수행됩니다.</p>
 *
 * <p><b>대상 플레이어 지정</b> (우선순위): {@code -Dpubg.target.accountId=...} 또는 환경변수 {@code PUBG_TARGET_ACCOUNT_ID}
 * &gt; {@code -Dpubg.target.playerName=...} 또는 {@code PUBG_TARGET_PLAYER_NAME} &gt; 아래 {@link #FALLBACK_PARTICIPANT_INDEX}.</p>
 */
@DisplayName("PubgPlayerMatchStats 수동 확인용 출력 테스트")
class PubgPlayerMatchStatsOutputTest {

    private static final String MATCH_SAMPLE_PATH = "src/test/resources/samples/pubg/pubg_match_sample.json";
    private static final String TELEMETRY_SAMPLE_PATH = "src/test/resources/samples/pubg/pubg_match_included_sample.json";
    private static final String OUTPUT_DIR = "target/pubg-stats-output";

    /**
     * accountId / playerName 을 지정하지 않았을 때만 사용하는 참가자 인덱스 (0 = 첫 번째).
     */
    private static final int FALLBACK_PARTICIPANT_INDEX = 0;

    /** 텔레메트리 스트리밍 시 상한(성능/속도 균형) */
    private static final int MAX_TELEMETRY_STREAM_EVENTS = 20_000;

    /** 타임라인 라인 수 (프롬프트 길이 통제) */
    private static final int MAX_TIMELINE_LINES = 30;

    @Test
    @DisplayName("PUBG 샘플 → PubgPlayerMatchStatsDTO → 프롬프트/JSON/텍스트 파일 출력")
    void outputSinglePlayerStatsAndFullPrompt() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        PubgJsonService jsonService = new PubgJsonService();
        PubgMatchMapper matchMapper = new PubgMatchMapper();

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        PubgMatchApiResponse matchDto = jsonService.parseMatchResponse(matchJson);
        assertThat(matchDto).isNotNull();

        PubgMatch match = matchMapper.toEntity(matchDto);
        assertThat(match).isNotNull();
        assertThat(match.getParticipants()).isNotEmpty();

        PubgMatchParticipant participant = resolveTargetParticipant(match.getParticipants());
        assertThat(participant).isNotNull();
        assertThat(participant.getPlayerId()).isNotBlank();

        List<PubgTelemetryEvent> playerEvents = loadPlayerEventsFromSample(
                match,
                participant.getPlayerId(),
                Paths.get(TELEMETRY_SAMPLE_PATH),
                MAX_TELEMETRY_STREAM_EVENTS
        );

        PubgTelemetryPromptTimelineBuilder timelineBuilder = new PubgTelemetryPromptTimelineBuilder(
                new ObjectMapper(),
                new PubgMapRegionMapper(new PubgMapConfigLoader())
        );

        List<com.gamematcher.dto.ai.evaluation.PubgTimelineEventLineDTO> timelineLines =
                timelineBuilder.buildTimelineLines(
                match.getMapName(),
                playerEvents,
                MAX_TIMELINE_LINES
        );

        PubgPlayerMatchStatsDTO stats = PubgPlayerMatchStatsDTO.builder()
                .game("PUBG")
                .matchId(match.getMatchId())
                .playerName(participant.getName())
                .accountId(participant.getPlayerId())
                .teamId(null)
                .winPlace(participant.getWinPlace())
                .won(participant.isWin())
                .kills(participant.getKills())
                .assists(participant.getAssists())
                .damageDealt(participant.getDamageDealt())
                .mapName(match.getMapName())
                .timelineLines(timelineLines)
                .build();

        ObjectMapper om = createOutputObjectMapper();
        Path jsonPath = outDir.resolve("pubg_single_player_match_stats.json");
        om.writeValue(jsonPath.toFile(), stats);
        System.out.println("JSON 출력: " + jsonPath.toAbsolutePath());

        PubgStatsToPromptFormatter formatter = new PubgStatsToPromptFormatter();
        String statsText = formatter.formatSummary(stats, MAX_TIMELINE_LINES);
        Path txtStatsPath = outDir.resolve("pubg_single_player_match_stats.txt");
        Files.writeString(txtStatsPath, "=== PubgPlayerMatchStatsDTO (formatSummary) ===\n\n" + statsText);
        System.out.println("텍스트 출력: " + txtStatsPath.toAbsolutePath());

        PubgEvaluationPromptBuilder promptBuilder = new PubgEvaluationPromptBuilder(formatter);
        String fullPrompt = promptBuilder.build(stats, MAX_TIMELINE_LINES);
        Path txtPromptPath = outDir.resolve("pubg_single_player_full_prompt.txt");
        Files.writeString(txtPromptPath, "=== LLM 전송용 전체 프롬프트 ===\n\n" + fullPrompt);
        System.out.println("전체 프롬프트 출력: " + txtPromptPath.toAbsolutePath());

        assertThat(fullPrompt).contains("[Output Format]");
    }

    @Test
    @DisplayName("PUBG 샘플 → LLM API 평가 → 텍스트 파일 출력 (실제 API 키 필요 시 호출)")
    void outputLlmApiResponse() throws Exception {
        Path outDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outDir);

        String apiKey = resolveApiKeyForTest();

        ObjectMapper om = new ObjectMapper();
        LlmEvaluationService llmService = new LlmEvaluationServiceImpl(
                om,
                apiKey != null ? apiKey : "",
                "gpt-4o-mini",
                30,
                2
        );

        // 프롬프트 생성기
        PubgStatsToPromptFormatter formatter = new PubgStatsToPromptFormatter();
        PubgEvaluationPromptBuilder promptBuilder = new PubgEvaluationPromptBuilder(formatter);
        PubgLlmEvaluationService pubgLlmService = new PubgLlmEvaluationService(promptBuilder, llmService);

        // 1) 샘플 매치/텔레메트리로 단일 플레이어 stats 만들기
        PubgJsonService jsonService = new PubgJsonService();
        PubgMatchMapper matchMapper = new PubgMatchMapper();

        String matchJson = Files.readString(Paths.get(MATCH_SAMPLE_PATH));
        PubgMatchApiResponse matchDto = jsonService.parseMatchResponse(matchJson);
        PubgMatch match = matchMapper.toEntity(matchDto);
        assertThat(match).isNotNull();

        PubgMatchParticipant participant = resolveTargetParticipant(match.getParticipants());

        List<PubgTelemetryEvent> playerEvents = loadPlayerEventsFromSample(
                match,
                participant.getPlayerId(),
                Paths.get(TELEMETRY_SAMPLE_PATH),
                MAX_TELEMETRY_STREAM_EVENTS
        );

        PubgTelemetryPromptTimelineBuilder timelineBuilder = new PubgTelemetryPromptTimelineBuilder(
                new ObjectMapper(),
                new PubgMapRegionMapper(new PubgMapConfigLoader())
        );

        List<com.gamematcher.dto.ai.evaluation.PubgTimelineEventLineDTO> timelineLines =
                timelineBuilder.buildTimelineLines(
                        match.getMapName(),
                        playerEvents,
                        MAX_TIMELINE_LINES
                );

        PubgPlayerMatchStatsDTO stats = PubgPlayerMatchStatsDTO.builder()
                .game("PUBG")
                .matchId(match.getMatchId())
                .playerName(participant.getName())
                .accountId(participant.getPlayerId())
                .teamId(null)
                .winPlace(participant.getWinPlace())
                .won(participant.isWin())
                .kills(participant.getKills())
                .assists(participant.getAssists())
                .damageDealt(participant.getDamageDealt())
                .mapName(match.getMapName())
                .timelineLines(timelineLines)
                .build();

        // 2) 실제 LLM 호출(환경변수 apiKey 존재 시)
        Optional<LlmEvaluationResponseDTO> result = pubgLlmService.evaluate(stats, MAX_TIMELINE_LINES);

        StringBuilder content = new StringBuilder();
        content.append("=== LLM API 평가 결과 (PUBG, gpt-4o-mini) ===\n");
        content.append("플레이어: ").append(stats.getPlayerName()).append(" | accountId: ")
                .append(stats.getAccountId()).append("\n\n");

        if (result.isPresent()) {
            LlmEvaluationResponseDTO dto = result.get();
            content.append("[요약]\n").append(dto.getSummary()).append("\n\n");
            String comment = dto.getDetailedComment();
            content.append("[상세 코멘트]\n").append(comment != null ? comment.replace("\\n", "\n") : "")
                    .append("\n");
        } else {
            content.append("AI_API_KEY 또는 OPENAI_API_KEY가 설정되지 않았거나 API 호출에 실패했습니다.\n");
            content.append("환경변수를 설정하고 테스트를 다시 실행하세요.\n");
        }

        Path txtPath = outDir.resolve("pubg_llm_api_response.txt");
        Files.writeString(txtPath, content);
        System.out.println("LLM API 응답 출력: " + txtPath.toAbsolutePath());
    }

    private static List<PubgTelemetryEvent> loadPlayerEventsFromSample(
            PubgMatch match,
            String accountId,
            Path telemetryPath,
            int maxStreamEvents
    ) throws Exception {
        PubgTelemetryStreamExtractor extractor = new PubgTelemetryStreamExtractor();
        List<PubgTelemetryEvent> events = new ArrayList<>();

        try (InputStream in = Files.newInputStream(telemetryPath)) {
            extractor.iterateEvents(in, maxStreamEvents, row -> {
                if (row == null || row.getAccountId() == null) return;
                if (!row.getAccountId().equals(accountId)) return;
                events.add(toTelemetryEvent(match, row));
            });
        }

        return events;
    }

    private static PubgTelemetryEvent toTelemetryEvent(PubgMatch match, PubgTelemetryEventRowDto row) {
        PubgTelemetryEvent e = new PubgTelemetryEvent();
        e.setMatch(match);
        e.setEventSequence(row.getEventSequence());
        e.setEventType(row.getEventType());
        e.setEventTimestamp(row.getEventTimestamp());
        e.setAccountId(row.getAccountId());
        e.setTeamId(row.getTeamId());
        e.setLocationX(row.getLocationX());
        e.setLocationY(row.getLocationY());
        e.setLocationZ(row.getLocationZ());
        e.setIsInBlueZone(row.getIsInBlueZone());
        e.setItemId(row.getItemId());
        e.setItemCategory(row.getItemCategory());
        e.setPayloadJson(row.getPayloadJson());
        return e;
    }

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
        } catch (java.io.IOException | RuntimeException ignored) {
        }
        return "";
    }

    private static ObjectMapper createOutputObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om;
    }

    /**
     * 단일 출력 대상 참가자 결정. accountId &gt; playerName &gt; 인덱스.
     */
    private static PubgMatchParticipant resolveTargetParticipant(List<PubgMatchParticipant> participants) {
        assertThat(participants).isNotEmpty();

        String accountId = firstNonBlank(
                System.getProperty("pubg.target.accountId"),
                System.getenv("PUBG_TARGET_ACCOUNT_ID")
        );
        if (accountId != null) {
            Optional<PubgMatchParticipant> found = participants.stream()
                    .filter(p -> p != null && accountId.equals(p.getPlayerId()))
                    .findFirst();
            assertThat(found)
                    .as("pubg.target.accountId / PUBG_TARGET_ACCOUNT_ID=%s 에 해당하는 참가자", accountId)
                    .isPresent();
            return found.get();
        }

        String playerName = firstNonBlank(
                System.getProperty("pubg.target.playerName"),
                System.getenv("PUBG_TARGET_PLAYER_NAME")
        );
        if (playerName != null) {
            String target = playerName.trim();
            Optional<PubgMatchParticipant> found = participants.stream()
                    .filter(p -> p != null && p.getName() != null
                            && target.equalsIgnoreCase(p.getName().trim()))
                    .findFirst();
            assertThat(found)
                    .as("pubg.target.playerName / PUBG_TARGET_PLAYER_NAME=%s 에 해당하는 참가자", playerName)
                    .isPresent();
            return found.get();
        }

        int idx = Math.min(FALLBACK_PARTICIPANT_INDEX, participants.size() - 1);
        return participants.get(idx);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }
}

