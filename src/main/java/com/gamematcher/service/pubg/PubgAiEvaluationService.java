package com.gamematcher.service.pubg;

import com.gamematcher.constant.GameList;
import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.GenericStatsDTO;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.PubgPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.PubgTimelineEventLineDTO;
import com.gamematcher.dto.pubg.PubgAiEvaluationResponseDto;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchAiEvaluation;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import com.gamematcher.entity.match.pubg.PubgTelemetryEvent;
import com.gamematcher.mapper.PubgAiEvaluationMapper;
import com.gamematcher.repository.match.PubgMatchAiEvaluationRepository;
import com.gamematcher.repository.match.PubgMatchRepository;
import com.gamematcher.repository.match.PubgTelemetryEventRepository;
import com.gamematcher.service.ai.PubgLlmEvaluationService;
import com.gamematcher.service.ai.RuleBasedScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PubgAiEvaluationService {

    @Value("${ai.llm.model:gpt-5-mini}")
    private String defaultLlmModel;

    private final PubgMatchRepository matchRepository;
    private final PubgTelemetryEventRepository telemetryEventRepository;
    private final PubgTelemetryPromptTimelineBuilder timelineBuilder;
    private final PubgLlmEvaluationService llmEvaluationService;
    private final PubgMatchAiEvaluationRepository evaluationRepository;
    private final PubgAiEvaluationMapper evaluationMapper;
    private final RuleBasedScoreService ruleBasedScoreService;

    @Transactional
    public List<PubgAiEvaluationResponseDto> evaluateMatch(String matchId, int maxTimelineLines) {
        return evaluateMatch(matchId, maxTimelineLines, null, null, null, false);
    }

    @Transactional
    public List<PubgAiEvaluationResponseDto> evaluateMatch(
            String matchId,
            int maxTimelineLines,
            String accountId,
            String playerName
    ) {
        return evaluateMatch(matchId, maxTimelineLines, accountId, playerName, null, false);
    }

    /**
     * @param modelOverride 비어 있지 않으면 해당 LLM 모델로 호출·저장
     * @param force         true면 해당 모델 행이 있어도 LLM 재호출
     */
    @Transactional
    public List<PubgAiEvaluationResponseDto> evaluateMatch(
            String matchId,
            int maxTimelineLines,
            String accountId,
            String playerName,
            String modelOverride,
            boolean force
    ) {
        if (matchId == null || matchId.isBlank()) {
            return List.of();
        }

        PubgMatch match = matchRepository.findByMatchId(matchId).orElse(null);
        if (match == null || match.getParticipants() == null || match.getParticipants().isEmpty()) {
            return List.of();
        }

        List<PubgMatchParticipant> targets = filterParticipants(match.getParticipants(), accountId, playerName);
        if (targets.isEmpty()) {
            log.debug("PUBG AI 평가 대상 없음: matchId={}, accountId={}, playerName={}", matchId, accountId, playerName);
            return List.of();
        }

        String modelKey = effectiveModelKey(modelOverride);
        List<PubgAiEvaluationResponseDto> results = new ArrayList<>();
        for (PubgMatchParticipant p : targets) {
            if (p == null || p.getPlayerId() == null) {
                continue;
            }
            evaluateAndSaveForParticipant(match, p, maxTimelineLines, modelKey, force).ifPresent(results::add);
        }
        log.info("PUBG AI 평가 완료: matchId={}, model={}, evaluatedPlayers={}", matchId, modelKey, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public Optional<PubgAiEvaluationResponseDto> findSavedEvaluation(
            String matchId, String accountId, String modelOverride) {
        if (matchId == null || matchId.isBlank() || accountId == null || accountId.isBlank()) {
            return Optional.empty();
        }
        String aid = accountId.trim();
        String modelKey = effectiveModelKey(modelOverride);

        Optional<PubgMatchAiEvaluation> evalOpt = evaluationRepository
                .findByMatchIdAndAccountIdAndLlmModel(matchId, aid, modelKey);
        String defaultTrimmed = defaultLlmModel != null ? defaultLlmModel.trim() : "";
        if (evalOpt.isEmpty() && modelKey.equals(defaultTrimmed)) {
            evalOpt = evaluationRepository.findByMatchIdAndAccountIdAndLlmModel(matchId, aid, "");
            if (evalOpt.isEmpty()) {
                evalOpt = evaluationRepository.findByMatchIdAndAccountIdLegacyBlankModel(matchId, aid);
            }
        }
        if (evalOpt.isEmpty()) {
            return Optional.empty();
        }
        PubgMatchAiEvaluation e = evalOpt.get();
        String summary = e.getSummary();
        String detailed = e.getDetailedComment();
        boolean hasText = (summary != null && !summary.isBlank())
                || (detailed != null && !detailed.isBlank());
        if (!hasText && e.getGrade() == null && e.getScore() == null) {
            return Optional.empty();
        }
        return Optional.of(evaluationMapper.toDto(e, List.of()));
    }

    private Optional<PubgAiEvaluationResponseDto> evaluateAndSaveForParticipant(
            PubgMatch match,
            PubgMatchParticipant p,
            int maxTimelineLines,
            String modelKey,
            boolean force
    ) {
        String matchId = match.getMatchId();
        List<PubgTelemetryEvent> events = telemetryEventRepository
                .findByMatchMatchIdAndAccountIdOrderByEventTimestampAsc(matchId, p.getPlayerId());
        String mapName = match.getMapName();

        List<PubgTimelineEventLineDTO> lines = timelineBuilder.buildTimelineLines(
                mapName,
                events,
                maxTimelineLines
        );

        List<String> debugPreview = lines.stream()
                .limit(6)
                .map(l -> l.getLabel() + " " + l.getMessage())
                .toList();

        PubgPlayerMatchStatsDTO stats = PubgPlayerMatchStatsDTO.builder()
                .game("PUBG")
                .matchId(matchId)
                .playerName(p.getName())
                .accountId(p.getPlayerId())
                .teamId(null)
                .winPlace(p.getWinPlace())
                .won(p.isWin())
                .kills(p.getKills())
                .assists(p.getAssists())
                .damageDealt(p.getDamageDealt())
                .mapName(mapName)
                .timelineLines(lines)
                .build();

        Optional<PubgMatchAiEvaluation> existingOpt =
                evaluationRepository.findByPubgMatchParticipant_IdAndLlmModel(p.getId(), modelKey);
        if (existingOpt.isPresent() && !force) {
            return Optional.of(evaluationMapper.toDto(existingOpt.get(), debugPreview));
        }

        Optional<LlmEvaluationResponseDTO> llmOpt =
                llmEvaluationService.evaluate(stats, maxTimelineLines, modelKey);
        LlmEvaluationResponseDTO llm = llmOpt.orElseGet(LlmEvaluationResponseDTO::new);
        if (llmOpt.isEmpty()) {
            log.debug("PUBG LLM 미수행, 규칙 점수만 저장: accountId={}", p.getPlayerId());
        }

        Integer killsVal = p.getKills();
        Integer assistsVal = p.getAssists();
        int kills = killsVal != null ? killsVal : 0;
        int assists = assistsVal != null ? assistsVal : 0;
        int deaths = p.isWin() ? 0 : 1;
        MatchResult result = p.isWin() ? MatchResult.VICTORY : MatchResult.DEFEAT;

        GenericStatsDTO ruleStats = GenericStatsDTO.builder()
                .game(GameList.PUBG.name())
                .kills(kills)
                .deaths(deaths)
                .assists(assists)
                .result(result)
                .build();

        Integer score = ruleBasedScoreService.calculateScore(GameList.PUBG.name(), ruleStats).orElse(null);
        String summary = llm.getSummary();
        String detailedComment = llm.getDetailedComment();

        if (existingOpt.isPresent()) {
            PubgMatchAiEvaluation entity = existingOpt.get();
            entity.setLlmModel(modelKey);
            entity.setStatus(EvaluationStatus.COMPLETED);
            entity.setScore(score);
            entity.setGrade(Grade.fromScore(score));
            entity.setSummary(summary);
            entity.setDetailedComment(detailedComment);
            entity.setEvaluatedAt(LocalDateTime.now());
            entity = evaluationRepository.save(entity);
            return Optional.of(evaluationMapper.toDto(entity, debugPreview));
        }

        PubgMatchAiEvaluation entity = new PubgMatchAiEvaluation(
                p,
                modelKey,
                EvaluationStatus.COMPLETED,
                score,
                summary,
                detailedComment
        );
        try {
            entity = evaluationRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            Optional<PubgMatchAiEvaluation> recovered =
                    evaluationRepository.findByPubgMatchParticipant_IdAndLlmModel(p.getId(), modelKey);
            if (recovered.isPresent()) {
                log.debug("PUBG AI 평가 INSERT 경합, 기존 행 반환: participantId={}, model={}", p.getId(), modelKey);
                return Optional.of(evaluationMapper.toDto(recovered.get(), debugPreview));
            }
            throw ex;
        }
        return Optional.of(evaluationMapper.toDto(entity, debugPreview));
    }

    private String effectiveModelKey(String modelOverride) {
        String d = defaultLlmModel != null ? defaultLlmModel.trim() : "";
        if (modelOverride != null && !modelOverride.isBlank()) {
            return modelOverride.trim();
        }
        return d;
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
                    .filter(part -> part != null && id.equals(part.getPlayerId()))
                    .collect(Collectors.toList());
        }
        if (playerName != null && !playerName.isBlank()) {
            String name = playerName.trim().toLowerCase(Locale.ROOT);
            return all.stream()
                    .filter(part -> part != null && part.getName() != null
                            && name.equals(part.getName().trim().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return all.stream().filter(part -> part != null && part.getPlayerId() != null).collect(Collectors.toList());
    }
}
