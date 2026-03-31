package com.gamematcher.service.lol;

import com.gamematcher.constant.GameList;
import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.LolPlayerMatchStatsDTO;
import com.gamematcher.dto.lol.LolAiEvaluationResponseDto;
import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.lol.LolMatchTimelineDetailDto;
import com.gamematcher.entity.match.lol.LolMatch;
import com.gamematcher.entity.match.lol.LolMatchAiEvaluation;
import com.gamematcher.entity.match.lol.LolMatchParticipant;
import com.gamematcher.mapper.LolAiEvaluationMapper;
import com.gamematcher.mapper.LolMatchMapper;
import com.gamematcher.mapper.LolMatchStatsMapper;
import com.gamematcher.repository.match.LolMatchAiEvaluationRepository;
import com.gamematcher.repository.match.LolMatchParticipantRepository;
import com.gamematcher.repository.match.LolMatchRepository;
import com.gamematcher.service.ai.LolLlmEvaluationService;
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

/**
 * LoL 매치 참가자별 AI 평가 실행 및 {@link LolMatchAiEvaluation} 저장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LolAiEvaluationService {

    private static final String GAME_CODE = GameList.LEAGUE_OF_LEGENDS.name();

    @Value("${ai.llm.model:gpt-5-mini}")
    private String defaultLlmModel;

    private final LolMatchRepository lolMatchRepository;
    private final LolMatchParticipantRepository participantRepository;
    private final LolMatchAiEvaluationRepository evaluationRepository;
    private final LolMatchMapper lolMatchMapper;
    private final LolMatchStatsMapper lolMatchStatsMapper;
    private final LolLlmEvaluationService lolLlmEvaluationService;
    private final RuleBasedScoreService ruleBasedScoreService;
    private final LolAiEvaluationMapper evaluationMapper;

    @Transactional
    public List<LolAiEvaluationResponseDto> evaluateAndSaveByMatchId(String matchId) {
        return evaluateAndSaveByMatchId(matchId, null, null, null, false);
    }

    /**
     * @param puuid 비어 있지 않으면 해당 소환사만 평가
     * @param riotId {@code 닉#태그} 형태면 gameName/tagLine으로 필터
     */
    @Transactional
    public List<LolAiEvaluationResponseDto> evaluateAndSaveByMatchId(
            String matchId,
            String puuid,
            String riotId,
            String modelOverride,
            boolean force
    ) {
        if (matchId == null || matchId.isBlank()) {
            return List.of();
        }
        LolMatch match = lolMatchRepository.findByMatchIdWithParticipantsAndTimeline(matchId.trim()).orElse(null);
        if (match == null) {
            log.debug("LoL 매치 없음, AI 평가 스킵: {}", matchId);
            return List.of();
        }
        String gn = null;
        String tg = null;
        if (riotId != null && !riotId.isBlank()) {
            String t = riotId.trim();
            int hash = t.lastIndexOf('#');
            if (hash > 0 && hash < t.length() - 1) {
                gn = t.substring(0, hash).trim();
                tg = t.substring(hash + 1).trim();
            } else {
                gn = t;
            }
        }
        return evaluateAndSaveInternal(match, puuid, gn, tg, modelOverride, force);
    }

    @Transactional(readOnly = true)
    public Optional<LolAiEvaluationResponseDto> findSavedEvaluation(
            String matchId, String puuid, String modelOverride) {
        if (matchId == null || matchId.isBlank() || puuid == null || puuid.isBlank()) {
            return Optional.empty();
        }
        String pid = puuid.trim();
        Optional<LolMatchParticipant> playerOpt = participantRepository
                .findByMatch_MatchIdAndPuuidIgnoreCase(matchId, pid)
                .or(() -> participantRepository.findByMatch_MatchIdAndPuuid(matchId, pid));
        if (playerOpt.isEmpty()) {
            return Optional.empty();
        }
        LolMatchParticipant player = playerOpt.get();
        String modelKey = effectiveModelKey(modelOverride);
        Optional<LolMatchAiEvaluation> evalOpt = evaluationRepository
                .findByLolMatchParticipant_IdAndLlmModel(player.getId(), modelKey);
        String defaultTrimmed = defaultLlmModel != null ? defaultLlmModel.trim() : "";
        if (evalOpt.isEmpty() && modelKey.equals(defaultTrimmed)) {
            evalOpt = evaluationRepository.findByLolMatchParticipant_IdAndLlmModel(player.getId(), "");
        }
        if (evalOpt.isEmpty()) {
            return Optional.empty();
        }
        LolMatchAiEvaluation e = evalOpt.get();
        String summary = e.getSummary();
        String detailed = e.getDetailedComment();
        boolean hasText = (summary != null && !summary.isBlank())
                || (detailed != null && !detailed.isBlank());
        if (!hasText && e.getGrade() == null && e.getScore() == null) {
            return Optional.empty();
        }
        return Optional.of(evaluationMapper.toDto(e));
    }

    private List<LolAiEvaluationResponseDto> evaluateAndSaveInternal(
            LolMatch match,
            String filterPuuid,
            String filterGameName,
            String filterTagLine,
            String modelOverride,
            boolean force
    ) {
        if (match.getParticipants() == null || match.getParticipants().isEmpty()) {
            return List.of();
        }

        List<LolMatchParticipant> targets = filterParticipants(
                match.getParticipants(), filterPuuid, filterGameName, filterTagLine);
        if (targets.isEmpty()) {
            log.debug("LoL AI 평가 대상 없음: matchId={}, puuid={}, riotId={}",
                    match.getMatchId(), filterPuuid,
                    filterGameName != null && filterTagLine != null
                            ? filterGameName + "#" + filterTagLine : filterGameName);
            return List.of();
        }

        LolMatchDetailDto matchDto = lolMatchMapper.toMatchDto(match);
        LolMatchTimelineDetailDto timelineDto = match.getTimeline() != null
                ? lolMatchMapper.toTimelineDetailDto(match.getTimeline())
                : null;
        List<LolPlayerMatchStatsDTO> allStats =
                lolMatchStatsMapper.toPlayerMatchStatsDtos(matchDto, timelineDto);

        List<LolAiEvaluationResponseDto> results = new ArrayList<>();
        for (LolMatchParticipant player : targets) {
            LolPlayerMatchStatsDTO playerStats = allStats.stream()
                    .filter(ps -> player.getPuuid() != null && ps.getPlayerPuuid() != null
                            && player.getPuuid().trim().equalsIgnoreCase(ps.getPlayerPuuid().trim()))
                    .findFirst()
                    .orElse(null);
            evaluateAndSaveForPlayer(player, playerStats, modelOverride, force).ifPresent(results::add);
        }

        log.info("LoL AI 평가 완료: matchId={}, 저장={}", match.getMatchId(), results.size());
        return results;
    }

    private Optional<LolAiEvaluationResponseDto> evaluateAndSaveForPlayer(
            LolMatchParticipant player,
            LolPlayerMatchStatsDTO playerStats,
            String modelOverride,
            boolean force) {
        if (player == null || playerStats == null) {
            return Optional.empty();
        }

        String modelKey = effectiveModelKey(modelOverride);

        Optional<LolMatchAiEvaluation> existingOpt =
                evaluationRepository.findByLolMatchParticipant_IdAndLlmModel(player.getId(), modelKey);
        if (existingOpt.isPresent() && !force) {
            log.debug("LoL 이미 평가됨, 스킵: participantId={}, llmModel={}", player.getId(), modelKey);
            return existingOpt.map(evaluationMapper::toDto);
        }

        int score = playerStats.getMatchStats() != null
                ? ruleBasedScoreService.calculateScore(GAME_CODE, playerStats.getMatchStats()).orElse(100)
                : 100;

        String defaultModel = defaultLlmModel != null ? defaultLlmModel.trim() : "";
        String llmModelParam = modelKey.equals(defaultModel) ? null : modelKey;
        Optional<LlmEvaluationResponseDTO> llmResult =
                lolLlmEvaluationService.evaluate(playerStats, -1, llmModelParam);
        String summary = llmResult.map(LlmEvaluationResponseDTO::getSummary).orElse(null);
        String detailedComment = llmResult.map(LlmEvaluationResponseDTO::getDetailedComment).orElse(null);

        if (llmResult.isEmpty()) {
            log.debug("LoL LLM 미수행, 규칙 점수만 저장: puuid={}", player.getPuuid());
        }

        if (existingOpt.isPresent()) {
            LolMatchAiEvaluation entity = existingOpt.get();
            entity.setLlmModel(modelKey);
            entity.setStatus(EvaluationStatus.COMPLETED);
            entity.setScore(score);
            entity.setGrade(Grade.fromScore(score));
            entity.setSummary(summary);
            entity.setDetailedComment(detailedComment);
            entity.setEvaluatedAt(LocalDateTime.now());
            entity = evaluationRepository.save(entity);
            return Optional.of(evaluationMapper.toDto(entity));
        }

        LolMatchAiEvaluation entity = new LolMatchAiEvaluation(
                player,
                modelKey,
                EvaluationStatus.COMPLETED,
                score,
                summary,
                detailedComment
        );
        try {
            entity = evaluationRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            Optional<LolMatchAiEvaluation> recovered =
                    evaluationRepository.findByLolMatchParticipant_IdAndLlmModel(player.getId(), modelKey);
            if (recovered.isPresent()) {
                log.debug("LoL AI 평가 INSERT 경합, 기존 행 반환: participantId={}, model={}",
                        player.getId(), modelKey);
                return recovered.map(evaluationMapper::toDto);
            }
            throw ex;
        }
        return Optional.of(evaluationMapper.toDto(entity));
    }

    private String effectiveModelKey(String modelOverride) {
        String d = defaultLlmModel != null ? defaultLlmModel.trim() : "";
        if (modelOverride != null && !modelOverride.isBlank()) {
            return modelOverride.trim();
        }
        return d;
    }

    private static List<LolMatchParticipant> filterParticipants(
            List<LolMatchParticipant> all,
            String filterPuuid,
            String filterGameName,
            String filterTagLine
    ) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        if (filterPuuid != null && !filterPuuid.isBlank()) {
            String id = filterPuuid.trim();
            return all.stream()
                    .filter(p -> p != null && p.getPuuid() != null
                            && id.equalsIgnoreCase(p.getPuuid().trim()))
                    .collect(Collectors.toList());
        }
        if (filterGameName != null && !filterGameName.isBlank()
                && filterTagLine != null && !filterTagLine.isBlank()) {
            String gn = filterGameName.trim().toLowerCase(Locale.ROOT);
            String tg = filterTagLine.trim().toLowerCase(Locale.ROOT);
            return all.stream()
                    .filter(p -> p != null
                            && p.getRiotIdGameName() != null
                            && gn.equals(p.getRiotIdGameName().trim().toLowerCase(Locale.ROOT))
                            && p.getRiotIdTagline() != null
                            && tg.equals(p.getRiotIdTagline().trim().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (filterGameName != null && !filterGameName.isBlank()) {
            String gn = filterGameName.trim().toLowerCase(Locale.ROOT);
            return all.stream()
                    .filter(p -> p != null && p.getRiotIdGameName() != null
                            && gn.equals(p.getRiotIdGameName().trim().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(all);
    }
}
