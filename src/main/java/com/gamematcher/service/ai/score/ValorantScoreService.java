package com.gamematcher.service.ai.score;

import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import com.gamematcher.entity.match.valorant.ValorantKillEvent;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.entity.match.valorant.ValorantMatchRound;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 발로란트 라운드 기반 점수 서비스.
 * 승리 기여도 기반으로 라운드별 점수를 계산하고, 매치 점수는 라운드 점수의 평균으로 한다.
 */
@Service
public class ValorantScoreService {

    private static final int BASE_SCORE = 100;

    private final KillContextExtractor killContextExtractor;
    private final ValorantRoundScoreEngine roundScoreEngine;
    private final RoundScoreInputBuilder roundScoreInputBuilder;

    public ValorantScoreService(KillContextExtractor killContextExtractor,
                               ValorantRoundScoreEngine roundScoreEngine,
                               RoundScoreInputBuilder roundScoreInputBuilder) {
        this.killContextExtractor = killContextExtractor;
        this.roundScoreEngine = roundScoreEngine;
        this.roundScoreInputBuilder = roundScoreInputBuilder;
    }

    /**
     * 승리 기여도 기반 라운드 점수 평균으로 매치 점수 계산.
     *
     * @param match       매치 (rounds, killEvents 포함)
     * @param playerPuuid 플레이어 puuid
     * @param roundStats  이 플레이어의 라운드별 스탯 (roundIndex 순)
     * @return 라운드 점수 평균. 참가 라운드 0이면 100 반환
     */
    public int calculate(ValorantMatch match, String playerPuuid,
                         List<ValorantRoundStatsDTO> roundStats) {
        if (match == null || match.getRounds() == null || playerPuuid == null || roundStats == null) {
            return BASE_SCORE;
        }
        Map<Integer, ValorantRoundStatsDTO> statsByRound = roundStats.stream()
                .collect(Collectors.toMap(ValorantRoundStatsDTO::getRoundIndex, rs -> rs, (a, b) -> a));

        List<Integer> roundScores = new java.util.ArrayList<>();
        List<ValorantKillEvent> allKills = match.getKillEvents() != null ? match.getKillEvents() : List.of();

        for (ValorantMatchRound round : match.getRounds()) {
            int rIdx = round.getRoundIndex() != null ? round.getRoundIndex() : -1;
            ValorantRoundStatsDTO playerRoundStats = statsByRound.get(rIdx);
            if (playerRoundStats == null) continue;

            List<ValorantKillEvent> roundKills = allKills.stream()
                    .filter(k -> k.getRoundNumber() != null && k.getRoundNumber().equals(rIdx))
                    .toList();

            var extractResult = killContextExtractor.extract(round, roundKills);
            var input = roundScoreInputBuilder.build(playerRoundStats, extractResult, round);
            int roundScore = roundScoreEngine.calculateRound(input);
            playerRoundStats.setRoundContributionScore(roundScore);
            roundScores.add(roundScore);
        }

        if (roundScores.isEmpty()) return BASE_SCORE;
        return (int) Math.round(roundScores.stream().mapToInt(Integer::intValue).average().orElse(BASE_SCORE));
    }

    /**
     * ValorantPlayerMatchStatsDTO와 Match로 라운드 기반 점수 계산.
     */
    public int calculate(ValorantPlayerMatchStatsDTO playerStats, ValorantMatch match) {
        if (playerStats == null || match == null) return BASE_SCORE;
        return calculate(match, playerStats.getPlayerPuuid(), playerStats.getRoundStats());
    }
}
