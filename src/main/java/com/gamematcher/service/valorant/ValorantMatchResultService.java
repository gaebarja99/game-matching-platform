package com.gamematcher.service.valorant;

import com.gamematcher.constant.GameList;
import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.BaseStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.entity.ai.evaluation.Game;
import com.gamematcher.entity.ai.evaluation.MatchRecord;
import com.gamematcher.entity.ai.evaluation.MatchRecordParticipant;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.mapper.ValorantMatchStatsMapper;
import com.gamematcher.entity.account.RiotAccount;
import com.gamematcher.entity.User;
import com.gamematcher.repository.account.RiotAccountRepository;
import com.gamematcher.repository.ai.evaluation.GameRepository;
import com.gamematcher.repository.ai.evaluation.MatchRecordParticipantRepository;
import com.gamematcher.repository.ai.evaluation.MatchRecordRepository;
import com.gamematcher.repository.match.ValorantMatchDetailRepository;
import com.gamematcher.dto.ai.evaluation.StatsConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * 발로란트 매치를 AI 평가 도메인(MatchRecord, MatchRecordParticipant)으로 저장하는 서비스.
 * RiotAccount로 연동된 유저만 참가자로 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValorantMatchResultService {

    private static final String GAME_CODE = GameList.VALORANT.name();

    private final ValorantMatchDetailRepository valorantMatchDetailRepository;
    private final ValorantMatchStatsMapper valorantMatchStatsMapper;
    private final StatsConverter statsConverter;
    private final GameRepository gameRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final MatchRecordParticipantRepository matchRecordParticipantRepository;
    private final RiotAccountRepository riotAccountRepository;

    /**
     * 매치 ID로 발로란트 매치 결과를 AI 평가 도메인에 저장한다.
     * 이미 MatchRecord가 있으면 스킵(멱등).
     *
     * @param matchId 발로란트 매치 ID
     * @return 저장된 MatchRecord, 없거나 실패 시 empty
     */
    @Transactional
    public Optional<MatchRecord> saveMatchResult(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return Optional.empty();
        }

        ValorantMatch match = valorantMatchDetailRepository.findByMatchId(matchId).orElse(null);
        if (match == null) {
            log.debug("Valorant 매치 없음, 결과 저장 스킵: {}", matchId);
            return Optional.empty();
        }

        return saveMatchResult(match);
    }

    /**
     * ValorantMatch 엔티티를 AI 평가 도메인에 저장한다.
     *
     * @param match 발로란트 매치
     * @return 저장된 MatchRecord
     */
    @Transactional
    public Optional<MatchRecord> saveMatchResult(ValorantMatch match) {
        if (match == null || match.getMatchId() == null) {
            return Optional.empty();
        }

        Game game = gameRepository.findByCode(GAME_CODE).orElse(null);
        if (game == null) {
            log.warn("Game(VALORANT) 미등록, 매치 결과 저장 스킵: {}", match.getMatchId());
            return Optional.empty();
        }

        Optional<MatchRecord> existing = matchRecordRepository.findByGame_CodeAndMatchId(GAME_CODE, match.getMatchId());
        if (existing.isPresent()) {
            log.debug("MatchRecord 이미 존재, 스킵: {}", match.getMatchId());
            return existing;
        }

        MatchResult result = resolveMatchResult(match);
        LocalDateTime playedAt = toLocalDateTime(match.getGameStart());

        MatchRecord record = new MatchRecord(
                game,
                match.getMatchId(),
                result,
                playedAt,
                null
        );
        record = matchRecordRepository.save(record);

        List<ValorantPlayerMatchStatsDTO> playerStatsList = valorantMatchStatsMapper.toPlayerMatchStatsDtos(match);
        int savedParticipants = 0;
        for (ValorantPlayerMatchStatsDTO playerStats : playerStatsList) {
            if (playerStats == null || playerStats.getMatchStats() == null) continue;

            Optional<RiotAccount> riotAccount = riotAccountRepository.findByPuuid(playerStats.getPlayerPuuid());
            if (riotAccount.isEmpty()) {
                log.trace("puuid 미연동 유저 스킵: {}", playerStats.getPlayerPuuid());
                continue;
            }

            User user = riotAccount.get().getUser();
            String role = playerStats.getAgent();
            BaseStatsDTO statsDto = playerStats.getMatchStats();
            String rawStats = statsConverter.toJson(statsDto);

            MatchRecordParticipant participant = new MatchRecordParticipant(record, user, role, rawStats);
            matchRecordParticipantRepository.save(participant);
            savedParticipants++;
        }

        log.info("발로란트 매치 결과 저장 완료: matchId={}, participants={}", match.getMatchId(), savedParticipants);
        return Optional.of(record);
    }

    private MatchResult resolveMatchResult(ValorantMatch match) {
        Boolean redWon = match.getRedHasWon();
        Boolean blueWon = match.getBlueHasWon();
        if (Boolean.TRUE.equals(redWon) || Boolean.TRUE.equals(blueWon)) {
            return MatchResult.VICTORY;
        }
        if (Boolean.FALSE.equals(redWon) && Boolean.FALSE.equals(blueWon)) {
            return MatchResult.DRAW;
        }
        return null;
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) return null;
        if (epochMillis < 1_000_000_000_000L) {
            epochMillis *= 1000;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
