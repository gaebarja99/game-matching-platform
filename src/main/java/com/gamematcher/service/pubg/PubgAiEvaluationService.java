package com.gamematcher.service.pubg;

import com.gamematcher.constant.GameList;
import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.GenericStatsDTO;
import com.gamematcher.dto.ai.evaluation.PubgPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.PubgTimelineEventLineDTO;
import com.gamematcher.dto.pubg.PubgAiEvaluationResponseDto;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchAiEvaluation;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import com.gamematcher.entity.match.pubg.PubgTelemetryEvent;
import com.gamematcher.repository.match.PubgMatchRepository;
import com.gamematcher.repository.match.PubgMatchAiEvaluationRepository;
import com.gamematcher.repository.match.PubgTelemetryEventRepository;
import com.gamematcher.mapper.PubgAiEvaluationMapper;
import com.gamematcher.service.ai.LlmEvaluationService;
import com.gamematcher.service.ai.PubgEvaluationPromptBuilder;
import com.gamematcher.service.ai.RuleBasedScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PubgAiEvaluationService {

    private final PubgMatchRepository matchRepository;
    private final PubgTelemetryEventRepository telemetryEventRepository;
    private final PubgTelemetryPromptTimelineBuilder timelineBuilder;
    private final PubgEvaluationPromptBuilder promptBuilder;
    private final LlmEvaluationService llmEvaluationService;
    private final PubgMatchAiEvaluationRepository evaluationRepository;
    private final PubgAiEvaluationMapper evaluationMapper;
    private final RuleBasedScoreService ruleBasedScoreService;

    public PubgAiEvaluationService(
            PubgMatchRepository matchRepository,
            PubgTelemetryEventRepository telemetryEventRepository,
            PubgTelemetryPromptTimelineBuilder timelineBuilder,
            PubgEvaluationPromptBuilder promptBuilder,
            LlmEvaluationService llmEvaluationService,
            PubgMatchAiEvaluationRepository evaluationRepository,
            PubgAiEvaluationMapper evaluationMapper,
            RuleBasedScoreService ruleBasedScoreService
    ) {
        this.matchRepository = matchRepository;
        this.telemetryEventRepository = telemetryEventRepository;
        this.timelineBuilder = timelineBuilder;
        this.promptBuilder = promptBuilder;
        this.llmEvaluationService = llmEvaluationService;
        this.evaluationRepository = evaluationRepository;
        this.evaluationMapper = evaluationMapper;
        this.ruleBasedScoreService = ruleBasedScoreService;
    }

    /**
     * 매치 내 모든 참가자를 평가합니다.
     */
    @Transactional
    public List<PubgAiEvaluationResponseDto> evaluateMatch(String matchId, int maxTimelineLines) {
        return evaluateMatch(matchId, maxTimelineLines, null, null);
    }

    /**
     * PUBG 매치 AI 평가.
     *
     * @param accountId PUBG account id ({@code account.xxx}). 비어 있으면 필터 안 함.
     * @param playerName 참가자 닉네임(대소문자 무시). {@code accountId}가 있으면 무시됩니다.
     *                    둘 다 비어 있으면 매치 전원 평가.
     */
    @Transactional
    public List<PubgAiEvaluationResponseDto> evaluateMatch(
            String matchId,
            int maxTimelineLines,
            String accountId,
            String playerName
    ) {
        if (matchId == null || matchId.isBlank()) return List.of();

        PubgMatch match = matchRepository.findByMatchId(matchId).orElse(null);
        if (match == null || match.getParticipants() == null || match.getParticipants().isEmpty()) {
            return List.of();
        }

        List<PubgMatchParticipant> targets = filterParticipants(match.getParticipants(), accountId, playerName);
        if (targets.isEmpty()) {
            log.debug("PUBG AI 평가 대상 없음: matchId={}, accountId={}, playerName={}", matchId, accountId, playerName);
            return List.of();
        }

        List<PubgAiEvaluationResponseDto> results = new ArrayList<>();
        for (PubgMatchParticipant p : targets) {
            if (p == null || p.getPlayerId() == null) continue;

            List<PubgTelemetryEvent> events = telemetryEventRepository
                    .findByMatchMatchIdAndAccountIdOrderByEventTimestampAsc(
                            matchId,
                            p.getPlayerId()
                    );
            String mapName = match.getMapName();

            List<PubgTimelineEventLineDTO> lines = timelineBuilder.buildTimelineLines(
                    mapName,
                    events,
                    maxTimelineLines
            );

            PubgPlayerMatchStatsDTO stats = PubgPlayerMatchStatsDTO.builder()
                    .game("PUBG")
                    .matchId(matchId)
                    .playerName(p.getName())
                    .accountId(p.getPlayerId())
                    .teamId(null) // MVP: participant 엔티티에서 teamId를 직접 쓰지 못할 수 있어 null로 둠
                    .winPlace(p.getWinPlace())
                    .won(p.isWin())
                    .kills(p.getKills())
                    .assists(p.getAssists())
                    .damageDealt(p.getDamageDealt())
                    .mapName(mapName)
                    .timelineLines(lines)
                    .build();

            List<String> debugPreview = lines.stream()
                    .limit(6)
                    .map(l -> l.getLabel() + " " + l.getMessage())
                    .toList();

            if (evaluationRepository.existsByPubgMatchParticipantId(p.getId())) {
                PubgMatchAiEvaluation saved = evaluationRepository
                        .findByPubgMatchParticipantId(p.getId())
                        .orElse(null);
                results.add(evaluationMapper.toDto(saved, debugPreview));
                continue;
            }

            String prompt = promptBuilder.build(stats, maxTimelineLines);
            Optional<LlmEvaluationResponseDTO> llmOpt = llmEvaluationService.evaluate(prompt, null);
            LlmEvaluationResponseDTO llm = llmOpt.orElseGet(LlmEvaluationResponseDTO::new);

            Integer killsVal = p.getKills();
            Integer assistsVal = p.getAssists();

            int kills = killsVal != null ? killsVal : 0;
            int assists = assistsVal != null ? assistsVal : 0;
            int deaths = p.isWin() ? 0 : 1; // GenericScoreEngine의 KDA 계산을 위한 최소 보정(LOSS=1 death 가정)
            MatchResult result = p.isWin() ? MatchResult.VICTORY : MatchResult.DEFEAT;

            GenericStatsDTO ruleStats = GenericStatsDTO.builder()
                    .game(GameList.PUBG.name())
                    .kills(kills)
                    .deaths(deaths)
                    .assists(assists)
                    .result(result)
                    .build();

            Integer score = ruleBasedScoreService.calculateScore(GameList.PUBG.name(), ruleStats).orElse(null);

            PubgMatchAiEvaluation entity = new PubgMatchAiEvaluation(
                    p,
                    EvaluationStatus.COMPLETED,
                    score,
                    llm.getSummary(),
                    llm.getDetailedComment()
            );

            entity = evaluationRepository.save(entity);
            results.add(evaluationMapper.toDto(entity, debugPreview));
        }
        log.info("PUBG AI 평가 완료: matchId={}, evaluatedPlayers={}", matchId, results.size());
        return results;
    }

    private static List<PubgMatchParticipant> filterParticipants(
            List<PubgMatchParticipant> all,
            String accountId,
            String playerName
    ) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        if (accountId != null && !accountId.isBlank()) {
            String id = accountId.trim();
            return all.stream()
                    .filter(p -> p != null && id.equals(p.getPlayerId()))
                    .collect(Collectors.toList());
        }
        if (playerName != null && !playerName.isBlank()) {
            String name = playerName.trim().toLowerCase(Locale.ROOT);
            return all.stream()
                    .filter(p -> p != null && p.getName() != null
                            && name.equals(p.getName().trim().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return all.stream().filter(p -> p != null && p.getPlayerId() != null).collect(Collectors.toList());
    }
}
