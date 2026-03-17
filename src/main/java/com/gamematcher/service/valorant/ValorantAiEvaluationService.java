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

import java.util.List;
import java.util.Optional;

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
        ValorantMatch match = valorantMatchDetailRepository.findByMatchId(matchId).orElse(null);
        if (match == null) {
            log.debug("Valorant 매치 없음, AI 평가 스킵: {}", matchId);
            return List.of();
        }
        return evaluateAndSave(match);
    }

    /**
     * 매치의 모든 플레이어를 AI 평가하고 저장한다.
     *
     * @param match 발로란트 매치
     * @return 저장된 평가 DTO 목록
     */
    @Transactional
    public List<ValorantAiEvaluationResponseDto> evaluateAndSave(ValorantMatch match) {
        if (match == null || match.getPlayers() == null || match.getPlayers().isEmpty()) {
            return List.of();
        }

        List<ValorantPlayerMatchStatsDTO> playerStatsList = valorantMatchStatsMapper.toPlayerMatchStatsDtos(match);
        List<ValorantAiEvaluationResponseDto> results = new java.util.ArrayList<>();

        for (ValorantMatchPlayer player : match.getPlayers()) {
            ValorantPlayerMatchStatsDTO playerStats = playerStatsList.stream()
                    .filter(ps -> player.getPuuid() != null && player.getPuuid().equals(ps.getPlayerPuuid()))
                    .findFirst()
                    .orElse(null);

            if (playerStats == null) {
                log.trace("플레이어 스탯 없음, 스킵: puuid={}", player.getPuuid());
                continue;
            }

            Optional<ValorantAiEvaluationResponseDto> saved = evaluateAndSaveForPlayer(player, playerStats);
            saved.ifPresent(results::add);
        }

        log.info("발로란트 AI 평가 완료: matchId={}, 저장된 평가={}", match.getMatchId(), results.size());
        return results;
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
        return evaluateAndSaveForPlayer(player, playerStats);
    }

    /**
     * 단일 플레이어에 대해 AI 평가를 실행하고 저장한다.
     */
    private Optional<ValorantAiEvaluationResponseDto> evaluateAndSaveForPlayer(
            ValorantMatchPlayer player,
            ValorantPlayerMatchStatsDTO playerStats) {
        if (player == null || playerStats == null) {
            return Optional.empty();
        }

        if (evaluationRepository.existsByValorantMatchPlayerId(player.getId())) {
            log.debug("이미 평가됨, 스킵: valorantMatchPlayerId={}", player.getId());
            return evaluationRepository.findByValorantMatchPlayerId(player.getId())
                    .map(evaluationMapper::toDto);
        }

        int score = playerStats.getMatchStats() != null
                ? playerStats.getMatchStats().getMatchAverageContributionScore()
                : 100;

        Optional<LlmEvaluationResponseDTO> llmResult = valorantLlmEvaluationService.evaluate(playerStats);
        String summary = llmResult.map(LlmEvaluationResponseDTO::getSummary).orElse(null);
        String detailedComment = llmResult.map(LlmEvaluationResponseDTO::getDetailedComment).orElse(null);

        if (llmResult.isEmpty()) {
            log.debug("LLM 평가 미수행(API 키 없음 또는 실패), 규칙 기반 점수만 저장: puuid={}", player.getPuuid());
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
