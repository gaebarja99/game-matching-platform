package com.gamematcher.service.pubg;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.pubg.PubgAiEvaluationResponseDto;
import com.gamematcher.dto.pubg.PubgMatchApiResponse;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchAiEvaluation;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import com.gamematcher.mapper.PubgMatchMapper;
import com.gamematcher.repository.match.PubgMatchAiEvaluationRepository;
import com.gamematcher.repository.match.PubgMatchRepository;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PUBG AI 평가 → DB 저장 흐름")
class PubgAiEvaluationFlowTest {

    private static final Path MATCH_JSON = Paths.get("src/test/resources/samples/pubg/pubg_match_sample.json");
    private static final Path TELEMETRY_JSON = Paths.get("src/test/resources/samples/pubg/pubg_match_included_sample.json");

    @Autowired
    PubgJsonService jsonService;

    @Autowired
    PubgMatchMapper matchMapper;

    @Autowired
    PubgMatchRepository matchRepository;

    @Autowired
    PubgTelemetrySaveService saveService;

    @Autowired
    PubgAiEvaluationService pubgAiEvaluationService;

    @Autowired
    PubgMatchAiEvaluationRepository evaluationRepository;

    @MockBean
    LlmEvaluationService llmEvaluationService;

    private PubgMatch savedMatch;

    @BeforeEach
    void init() throws Exception {
        String matchJson = Files.readString(MATCH_JSON);
        PubgMatchApiResponse matchDto = jsonService.parseMatchResponse(matchJson);

        PubgMatch matchEntity = matchMapper.toEntity(matchDto);
        assertThat(matchEntity).isNotNull();

        savedMatch = matchRepository.save(Objects.requireNonNull(matchEntity));
        assertThat(savedMatch.getMatchId()).isNotBlank();
        assertThat(savedMatch.getParticipants()).isNotEmpty();

        // 테스트 성능을 위해 일부만 저장
        saveService.saveTelemetryForMatchId(
                savedMatch.getMatchId(),
                TELEMETRY_JSON,
                200
        );
    }

    @Test
    @DisplayName("LLM 응답 → DB 저장: 평가 엔티티에 summary/detailedComment 저장됨")
    void evaluateAndSave_storesLlmResponseToDb() {
        LlmEvaluationResponseDTO mockLlmResponse = new LlmEvaluationResponseDTO();
        mockLlmResponse.setSummary("테스트 AI 요약");
        mockLlmResponse.setDetailedComment("테스트 AI 상세 코멘트");

        when(llmEvaluationService.evaluate(anyString(), isNull())).thenReturn(Optional.of(mockLlmResponse));

        List<PubgAiEvaluationResponseDto> results = pubgAiEvaluationService
                .evaluateMatch(savedMatch.getMatchId(), 30);

        assertThat(results).isNotEmpty();

        PubgMatchParticipant firstParticipant = savedMatch.getParticipants().get(0);
        PubgMatchAiEvaluation savedEntity = evaluationRepository
                .findByPubgMatchParticipantId(firstParticipant.getId())
                .orElseThrow();

        assertThat(savedEntity.getSummary()).isEqualTo(mockLlmResponse.getSummary());
        assertThat(savedEntity.getDetailedComment()).isEqualTo(mockLlmResponse.getDetailedComment());
        assertThat(savedEntity.getScore()).isNotNull();
        assertThat(savedEntity.getGrade()).isNotNull();
        assertThat(savedEntity.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    @DisplayName("LLM 실패 시 규칙 기반 점수만 저장 (summary/detailedComment는 null)")
    void evaluateAndSave_whenLlmFails_savesRuleBasedScoreOnly() {
        when(llmEvaluationService.evaluate(anyString(), isNull())).thenReturn(Optional.empty());

        pubgAiEvaluationService.evaluateMatch(savedMatch.getMatchId(), 30);

        PubgMatchParticipant firstParticipant = savedMatch.getParticipants().get(0);
        PubgMatchAiEvaluation savedEntity = evaluationRepository
                .findByPubgMatchParticipantId(firstParticipant.getId())
                .orElseThrow();

        assertThat(savedEntity.getSummary()).isNull();
        assertThat(savedEntity.getDetailedComment()).isNull();
        assertThat(savedEntity.getScore()).isNotNull();
        assertThat(savedEntity.getGrade()).isNotNull();
        assertThat(savedEntity.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    }
}

