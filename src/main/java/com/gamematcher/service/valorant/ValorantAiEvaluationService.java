package com.gamematcher.service.valorant;

import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.dto.valorant.ValorantAiEvaluationResponseDto;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.entity.match.valorant.ValorantMatchAiEvaluation;
import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;
import com.gamematcher.mapper.ValorantAiEvaluationMapper;
import com.gamematcher.mapper.ValorantMatchStatsMapper;
import com.gamematcher.repository.match.ValorantMatchAiEvaluationRepository;
import com.gamematcher.repository.match.ValorantMatchDetailRepository;
import com.gamematcher.repository.match.ValorantMatchPlayerRepository;
import com.gamematcher.service.ai.ValorantLlmEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 발로란트 AI 평가 실행 및 ValorantMatchAiEvaluation 저장.
 * ValorantAiEvaluationResponseDto 구조로 평가 결과를 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValorantAiEvaluationService {

    private final ValorantMatchDetailRepository valorantMatchDetailRepository;
    private final ValorantMatchPlayerRepository valorantMatchPlayerRepository;
    private final ValorantMatchStatsMapper valorantMatchStatsMapper;
    private final ValorantLlmEvaluationService valorantLlmEvaluationService;
    private final ValorantMatchAiEvaluationRepository evaluationRepository;
    private final ValorantAiEvaluationMapper evaluationMapper;

    /**
     * 매치 ID로 해당 매치의 모든 플레이어를 AI 평가하고 저장한다.
     *
     * @param matchId 발로란트 매치 ID
     * @return 저장된 평가 DTO 목록
     */
    @Transactional
    public List<ValorantAiEvaluationResponseDto> evaluateAndSaveByMatchId(String matchId) {
        return evaluateAndSaveByMatchId(matchId, null, null, null, null, false);
    }

    /**
     * 매치 ID로 지정한 플레이어만 AI 평가하고 저장한다.
     *
     * <p>우선순위: {@code puuid} &gt; ({@code gameName} + {@code tagLine}). 파라미터가 모두 비어 있으면 전원.</p>
     *
     * @param puuid Riot PUUID (정확 일치)
     * @param gameName Riot 게임 닉 (태그와 함께 쓰면 정확 일치, 태그 없으면 닉만 대소문자 무시 일치 — 동명이인 시 첫 명만)
     * @param tagLine Riot 태그 (# 제외)
     */
    @Transactional
    public List<ValorantAiEvaluationResponseDto> evaluateAndSaveByMatchId(
            String matchId,
            String puuid,
            String gameName,
            String tagLine
    ) {
        return evaluateAndSaveByMatchId(matchId, puuid, gameName, tagLine, null, false);
    }

    /**
     * @param modelOverride 비어 있지 않으면 LLM에 해당 모델 지정
     * @param force          true면 이미 평가가 있어도 LLM을 다시 호출해 덮어씀
     */
    @Transactional
    public List<ValorantAiEvaluationResponseDto> evaluateAndSaveByMatchId(
            String matchId,
            String puuid,
            String gameName,
            String tagLine,
            String modelOverride,
            boolean force
    ) {
        ValorantMatch match = valorantMatchDetailRepository.findByMatchIdWithPlayers(matchId).orElse(null);
        if (match == null) {
            log.debug("Valorant 매치 없음, AI 평가 스킵: {}", matchId);
            return List.of();
        }
        return evaluateAndSaveInternal(match, puuid, gameName, tagLine, modelOverride, force);
    }

    /**
     * 매치의 모든 플레이어를 AI 평가하고 저장한다.
     *
     * @param match 발로란트 매치
     * @return 저장된 평가 DTO 목록
     */
    @Transactional
    public List<ValorantAiEvaluationResponseDto> evaluateAndSave(ValorantMatch match) {
        return evaluateAndSaveInternal(match, null, null, null, null, false);
    }

    private List<ValorantAiEvaluationResponseDto> evaluateAndSaveInternal(
            ValorantMatch match,
            String filterPuuid,
            String filterGameName,
            String filterTagLine,
            String modelOverride,
            boolean force
    ) {
        if (match == null || match.getPlayers() == null || match.getPlayers().isEmpty()) {
            return List.of();
        }

        List<ValorantMatchPlayer> targets = filterValorantPlayers(match.getPlayers(), filterPuuid, filterGameName, filterTagLine);
        if (targets.isEmpty()) {
            log.debug("발로란트 AI 평가 대상 없음: matchId={}, puuid={}, gameName={}, tagLine={}",
                    match.getMatchId(), filterPuuid, filterGameName, filterTagLine);
            return List.of();
        }

        List<ValorantPlayerMatchStatsDTO> playerStatsList = valorantMatchStatsMapper.toPlayerMatchStatsDtos(match);
        List<ValorantAiEvaluationResponseDto> results = new ArrayList<>();

        for (ValorantMatchPlayer player : targets) {
            ValorantPlayerMatchStatsDTO playerStats = playerStatsList.stream()
                    .filter(ps -> player.getPuuid() != null && player.getPuuid().equals(ps.getPlayerPuuid()))
                    .findFirst()
                    .orElse(null);

            if (playerStats == null) {
                log.trace("플레이어 스탯 없음, 스킵: puuid={}", player.getPuuid());
                continue;
            }

            Optional<ValorantAiEvaluationResponseDto> saved =
                    evaluateAndSaveForPlayer(player, playerStats, modelOverride, force);
            saved.ifPresent(results::add);
        }

        log.info("발로란트 AI 평가 완료: matchId={}, 저장된 평가={}", match.getMatchId(), results.size());
        return results;
    }

    private static List<ValorantMatchPlayer> filterValorantPlayers(
            List<ValorantMatchPlayer> all,
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
                    .filter(p -> p != null && id.equals(p.getPuuid()))
                    .collect(Collectors.toList());
        }
        if (filterGameName != null && !filterGameName.isBlank()
                && filterTagLine != null && !filterTagLine.isBlank()) {
            String gn = filterGameName.trim().toLowerCase(Locale.ROOT);
            String tg = filterTagLine.trim().toLowerCase(Locale.ROOT);
            return all.stream()
                    .filter(p -> p != null
                            && p.getName() != null && gn.equals(p.getName().trim().toLowerCase(Locale.ROOT))
                            && p.getTag() != null && tg.equals(p.getTag().trim().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (filterGameName != null && !filterGameName.isBlank()) {
            String gn = filterGameName.trim().toLowerCase(Locale.ROOT);
            return all.stream()
                    .filter(p -> p != null && p.getName() != null
                            && gn.equals(p.getName().trim().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(all);
    }

    /**
     * 단일 플레이어를 AI 평가하고 저장한다.
     *
     * @param valorantMatchPlayerId ValorantMatchPlayer ID
     * @return 저장된 평가 DTO, 없거나 실패 시 empty
     */
    @Transactional
    public Optional<ValorantAiEvaluationResponseDto> evaluateAndSaveByPlayerId(Long valorantMatchPlayerId) {
        ValorantMatchPlayer player = valorantMatchPlayerRepository.findById(valorantMatchPlayerId).orElse(null);
        if (player == null) {
            log.debug("ValorantMatchPlayer 없음: id={}", valorantMatchPlayerId);
            return Optional.empty();
        }
        ValorantMatch match = player.getMatch();
        List<ValorantPlayerMatchStatsDTO> playerStatsList = valorantMatchStatsMapper.toPlayerMatchStatsDtos(match);
        ValorantPlayerMatchStatsDTO playerStats = playerStatsList.stream()
                .filter(ps -> player.getPuuid() != null && player.getPuuid().equals(ps.getPlayerPuuid()))
                .findFirst()
                .orElse(null);
        return evaluateAndSaveForPlayer(player, playerStats, null, false);
    }

    /**
     * 단일 플레이어에 대해 AI 평가를 실행하고 저장한다.
     */
    private Optional<ValorantAiEvaluationResponseDto> evaluateAndSaveForPlayer(
            ValorantMatchPlayer player,
            ValorantPlayerMatchStatsDTO playerStats,
            String modelOverride,
            boolean force) {
        if (player == null || playerStats == null) {
            return Optional.empty();
        }

        Optional<ValorantMatchAiEvaluation> existingOpt =
                evaluationRepository.findByValorantMatchPlayerId(player.getId());
        if (existingOpt.isPresent() && !force) {
            log.debug("이미 평가됨, 스킵: valorantMatchPlayerId={}", player.getId());
            return existingOpt.map(evaluationMapper::toDto);
        }

        int score = playerStats.getMatchStats() != null
                ? playerStats.getMatchStats().getMatchAverageContributionScore()
                : 100;

        Optional<LlmEvaluationResponseDTO> llmResult =
                valorantLlmEvaluationService.evaluate(playerStats, -1, modelOverride);
        String summary = llmResult.map(LlmEvaluationResponseDTO::getSummary).orElse(null);
        String detailedComment = llmResult.map(LlmEvaluationResponseDTO::getDetailedComment).orElse(null);

        if (llmResult.isEmpty()) {
            log.debug("LLM 평가 미수행(API 키 없음 또는 실패), 규칙 기반 점수만 저장: puuid={}", player.getPuuid());
        }

        if (existingOpt.isPresent()) {
            ValorantMatchAiEvaluation entity = existingOpt.get();
            entity.setStatus(EvaluationStatus.COMPLETED);
            entity.setScore(score);
            entity.setGrade(Grade.fromScore(score));
            entity.setSummary(summary);
            entity.setDetailedComment(detailedComment);
            entity.setEvaluatedAt(LocalDateTime.now());
            entity = evaluationRepository.save(entity);
            return Optional.of(evaluationMapper.toDto(entity));
        }

        ValorantMatchAiEvaluation entity = new ValorantMatchAiEvaluation(
                player,
                EvaluationStatus.COMPLETED,
                score,
                summary,
                detailedComment
        );
        entity = evaluationRepository.save(entity);

        return Optional.of(evaluationMapper.toDto(entity));
    }

    /**
     * ValorantAiEvaluationResponseDto를 DB에 저장한다.
     * matchId + playerPuuid로 ValorantMatchPlayer를 조회하여 ValorantMatchAiEvaluation에 저장.
     * 이미 평가가 있으면 업데이트, 없으면 새로 생성한다.
     *
     * @param dto 저장할 AI 평가 DTO
     * @return 저장된 DTO, matchId/playerPuuid에 해당하는 플레이어가 없으면 empty
     */
    @Transactional
    public Optional<ValorantAiEvaluationResponseDto> saveFromDto(ValorantAiEvaluationResponseDto dto) {
        if (dto == null || dto.getMatchId() == null || dto.getPlayerPuuid() == null) {
            return Optional.empty();
        }

        ValorantMatchPlayer player = valorantMatchPlayerRepository
                .findByMatch_MatchIdAndPuuid(dto.getMatchId(), dto.getPlayerPuuid())
                .orElse(null);
        if (player == null) {
            log.debug("ValorantMatchPlayer 없음: matchId={}, puuid={}", dto.getMatchId(), dto.getPlayerPuuid());
            return Optional.empty();
        }

        ValorantMatchAiEvaluation entity = evaluationRepository.findByValorantMatchPlayerId(player.getId())
                .orElse(null);

        if (entity == null) {
            entity = new ValorantMatchAiEvaluation(player);
        }

        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : EvaluationStatus.COMPLETED);
        entity.setScore(dto.getScore());
        entity.setGrade(dto.getGrade() != null ? dto.getGrade() : (dto.getScore() != null ? com.gamematcher.constant.ai.evaluation.Grade.fromScore(dto.getScore()) : null));
        entity.setSummary(dto.getSummary());
        entity.setDetailedComment(dto.getDetailedComment());
        if (dto.getEvaluatedAt() != null) {
            entity.setEvaluatedAt(dto.getEvaluatedAt());
        } else if (entity.getEvaluatedAt() == null && (dto.getSummary() != null || dto.getDetailedComment() != null)) {
            entity.setEvaluatedAt(java.time.LocalDateTime.now());
        }

        entity = evaluationRepository.save(entity);
        return Optional.of(evaluationMapper.toDto(entity));
    }
}
