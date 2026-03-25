package com.gamematcher.service.valorant;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.valorant.ValorantAiEvaluationResponseDto;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.entity.match.valorant.ValorantMatchAiEvaluation;
import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;
import com.gamematcher.mapper.ValorantMatchMapper;
import com.gamematcher.repository.match.ValorantMatchAiEvaluationRepository;
import com.gamematcher.repository.match.ValorantMatchDetailRepository;
import com.gamematcher.service.ai.LlmEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * AI 응답 → DTO → DB 저장 흐름 검증 테스트.
 * - saveFromDto: DTO를 DB에 직접 저장
 * - evaluateAndSave: AI 응답(LlmEvaluationResponseDTO) → ValorantMatchAiEvaluation 저장
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Valorant AI 평가 응답 → DTO → DB 저장 흐름")
class ValorantAiEvaluationFlowTest {

    private static final String SAMPLE_PATH = "src/test/resources/samples/valorant/valorant_match_sample.json";

    @Autowired
    ValorantMatchDetailRepository valorantMatchDetailRepository;
    @Autowired
    ValorantMatchAiEvaluationRepository evaluationRepository;
    @Autowired
    ValorantAiEvaluationService valorantAiEvaluationService;
    @Autowired
    ValorantMatchJsonService valorantMatchJsonService;
    @Autowired
    ValorantMatchMapper valorantMatchMapper;

    @MockBean
    LlmEvaluationService llmEvaluationService;

    private ValorantMatch savedMatch;

    @BeforeEach
    void setUp() throws Exception {
        String json = Files.readString(Paths.get(SAMPLE_PATH));
        var matchDto = valorantMatchJsonService.parseFirstMatch(json);
        assertThat(matchDto).isNotNull();

        ValorantMatch match = valorantMatchMapper.toEntity(matchDto);
        assertThat(match).isNotNull();

        savedMatch = valorantMatchDetailRepository.save(match);
        assertThat(savedMatch.getMatchId()).isNotNull();
        assertThat(savedMatch.getPlayers()).isNotEmpty();
    }

    @Test
    @DisplayName("DTO → DB 저장: saveFromDto로 ValorantAiEvaluationResponseDto를 DB에 저장")
    void saveFromDto_storesDtoToDb() {
        ValorantMatchPlayer firstPlayer = savedMatch.getPlayers().get(0);
        String matchId = savedMatch.getMatchId();
        String puuid = firstPlayer.getPuuid();

        ValorantAiEvaluationResponseDto dto = ValorantAiEvaluationResponseDto.builder()
                .matchId(matchId)
                .playerPuuid(puuid)
                .playerDisplayName(firstPlayer.getName() + "#" + firstPlayer.getTag())
                .agent(firstPlayer.getAgent())
                .team(firstPlayer.getTeam())
                .status(EvaluationStatus.COMPLETED)
                .score(125)
                .grade(Grade.A)
                .summary("테스트 AI 요약: KDA와 팀 기여도가 양호했습니다.")
                .detailedComment("테스트 AI 상세 코멘트: 라운드당 평균 피해량과 KAST가 팀 평균 이상이었습니다.")
                .evaluatedAt(LocalDateTime.now())
                .build();

        Optional<ValorantAiEvaluationResponseDto> result = valorantAiEvaluationService.saveFromDto(dto);

        assertThat(result).isPresent();
        assertThat(result.get().getMatchId()).isEqualTo(matchId);
        assertThat(result.get().getPlayerPuuid()).isEqualTo(puuid);
        assertThat(result.get().getSummary()).isEqualTo(dto.getSummary());
        assertThat(result.get().getDetailedComment()).isEqualTo(dto.getDetailedComment());
        assertThat(result.get().getScore()).isEqualTo(125);
        assertThat(result.get().getGrade()).isEqualTo(Grade.A);

        ValorantMatchAiEvaluation savedEntity = evaluationRepository
                .findByValorantMatchPlayerId(firstPlayer.getId())
                .orElseThrow();
        assertThat(savedEntity.getSummary()).isEqualTo(dto.getSummary());
        assertThat(savedEntity.getDetailedComment()).isEqualTo(dto.getDetailedComment());
        assertThat(savedEntity.getScore()).isEqualTo(125);
        assertThat(savedEntity.getGrade()).isEqualTo(Grade.A);
        assertThat(savedEntity.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    @DisplayName("AI 응답 → DB 저장: evaluateAndSave로 Llm 응답이 ValorantMatchAiEvaluation에 저장됨")
    void evaluateAndSave_storesLlmResponseToDb() {
        LlmEvaluationResponseDTO mockLlmResponse = new LlmEvaluationResponseDTO();
        mockLlmResponse.setSummary("AI가 생성한 요약: 에이전트 활용과 포지셔닝이 우수했습니다.");
        mockLlmResponse.setDetailedComment("AI가 생성한 상세 코멘트: 팀원과의 협동과 오브젝티브 참여도가 높았습니다.");

        when(llmEvaluationService.evaluate(anyString())).thenReturn(Optional.of(mockLlmResponse));

        var results = valorantAiEvaluationService.evaluateAndSaveByMatchId(savedMatch.getMatchId());

        assertThat(results).isNotEmpty();

        ValorantMatchPlayer firstPlayer = savedMatch.getPlayers().get(0);
        ValorantMatchAiEvaluation savedEntity = evaluationRepository
                .findByValorantMatchPlayerId(firstPlayer.getId())
                .orElseThrow();

        assertThat(savedEntity.getSummary()).isEqualTo(mockLlmResponse.getSummary());
        assertThat(savedEntity.getDetailedComment()).isEqualTo(mockLlmResponse.getDetailedComment());
        assertThat(savedEntity.getScore()).isNotNull();
        assertThat(savedEntity.getGrade()).isNotNull();
        assertThat(savedEntity.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    @DisplayName("AI 응답 → DTO → DB: 전체 파이프라인 동작 검증")
    void fullPipeline_aiResponseToDtoToDb() {
        LlmEvaluationResponseDTO mockLlmResponse = new LlmEvaluationResponseDTO();
        mockLlmResponse.setSummary("파이프라인 검증용 AI 요약");
        mockLlmResponse.setDetailedComment("파이프라인 검증용 AI 상세 코멘트입니다.");

        when(llmEvaluationService.evaluate(anyString())).thenReturn(Optional.of(mockLlmResponse));

        var results = valorantAiEvaluationService.evaluateAndSave(savedMatch);

        assertThat(results).hasSize(savedMatch.getPlayers().size());

        for (ValorantAiEvaluationResponseDto dto : results) {
            assertThat(dto.getMatchId()).isEqualTo(savedMatch.getMatchId());
            assertThat(dto.getSummary()).isEqualTo(mockLlmResponse.getSummary());
            assertThat(dto.getDetailedComment()).isEqualTo(mockLlmResponse.getDetailedComment());
            assertThat(dto.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        }

        for (ValorantMatchPlayer player : savedMatch.getPlayers()) {
            ValorantMatchAiEvaluation entity = evaluationRepository
                    .findByValorantMatchPlayerId(player.getId())
                    .orElseThrow();
            assertThat(entity.getSummary()).isEqualTo(mockLlmResponse.getSummary());
            assertThat(entity.getDetailedComment()).isEqualTo(mockLlmResponse.getDetailedComment());
        }
    }

    @Test
    @DisplayName("LLM 실패 시 규칙 기반 점수만 저장 (summary/detailedComment는 null)")
    void evaluateAndSave_whenLlmFails_savesRuleBasedScoreOnly() {
        when(llmEvaluationService.evaluate(anyString())).thenReturn(Optional.empty());

        var results = valorantAiEvaluationService.evaluateAndSaveByMatchId(savedMatch.getMatchId());

        assertThat(results).isNotEmpty();

        ValorantMatchPlayer firstPlayer = savedMatch.getPlayers().get(0);
        ValorantMatchAiEvaluation savedEntity = evaluationRepository
                .findByValorantMatchPlayerId(firstPlayer.getId())
                .orElseThrow();

        assertThat(savedEntity.getSummary()).isNull();
        assertThat(savedEntity.getDetailedComment()).isNull();
        assertThat(savedEntity.getScore()).isNotNull();
        assertThat(savedEntity.getGrade()).isNotNull();
        assertThat(savedEntity.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    }
}
