package com.gamematcher.service.ai.score;

import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import com.gamematcher.dto.valorant.ValorantAssistContext;
import com.gamematcher.dto.valorant.ValorantKillContext;
import com.gamematcher.dto.valorant.ValorantRoundScoreInput;
import com.gamematcher.entity.match.valorant.ValorantMatchRound;
import com.gamematcher.entity.match.valorant.ValorantMatchRoundPlayer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RoundScoreInput 빌더. KillContextExtractor 결과와 라운드 스탯을 조합.
 */
@Component
public class RoundScoreInputBuilder {

    /**
     * 플레이어 1명의 라운드 1개에 대한 RoundScoreInput 생성.
     *
     * @param playerRoundStats 해당 라운드의 이 플레이어 스탯 (damageToEliminated, myDamagePerKill 포함)
     * @param extractResult    KillContextExtractor.extract() 결과
     * @param round            라운드 (팀별 로드아웃·팀원 puuid)
     * @return ValorantRoundScoreInput
     */
    public ValorantRoundScoreInput build(
            ValorantRoundStatsDTO playerRoundStats,
            KillContextExtractor.ExtractResult extractResult,
            ValorantMatchRound round
    ) {
        if (playerRoundStats == null || round == null || round.getPlayerStats() == null) {
            return ValorantRoundScoreInput.of(playerRoundStats, List.of(), List.of(),
                    Map.of(), Map.of(), 0, 0, null, List.of(), Set.of());
        }

        String playerPuuid = playerRoundStats.getPlayerPuuid();
        String myTeam = round.getPlayerStats().stream()
                .filter(rp -> playerPuuid != null && playerPuuid.equals(rp.getPlayerPuuid()))
                .findFirst()
                .map(ValorantMatchRoundPlayer::getPlayerTeam)
                .orElse(null);

        List<ValorantKillContext> myKills = extractResult.killsByKiller()
                .getOrDefault(playerPuuid, List.of());
        List<ValorantAssistContext> myAssists = extractResult.assistsByAssistant()
                .getOrDefault(playerPuuid, List.of());

        Map<String, Integer> damageToEliminated = playerRoundStats.getDamageToEliminated();
        Map<String, Integer> myDamagePerKill = playerRoundStats.getMyDamagePerKill();

        // 팀별 로드아웃
        Map<String, Integer> teamLoadout = new HashMap<>();
        Map<String, Set<String>> teamPuuids = new HashMap<>();
        for (ValorantMatchRoundPlayer rp : round.getPlayerStats()) {
            String team = rp.getPlayerTeam();
            if (team == null) continue;
            int loadout = rp.getLoadoutValue() != null ? rp.getLoadoutValue() : 0;
            teamLoadout.merge(team, loadout, Integer::sum);
            teamPuuids.computeIfAbsent(team, t -> new HashSet<>()).add(rp.getPlayerPuuid());
        }

        int ourLoadout = myTeam != null ? teamLoadout.getOrDefault(myTeam, 0) : 0;
        int theirLoadout = teamLoadout.entrySet().stream()
                .filter(e -> !Objects.equals(e.getKey(), myTeam))
                .mapToInt(Map.Entry::getValue)
                .sum();

        // 전체 킬 목록 (시간순)
        List<ValorantKillContext> allKills = extractResult.killsByKiller().values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparingInt(ValorantKillContext::killTimeInRound))
                .toList();

        // 내 사망 킬 찾기
        ValorantKillContext myDeath = allKills.stream()
                .filter(k -> playerPuuid != null && playerPuuid.equals(k.victimPuuid()))
                .findFirst()
                .orElse(null);

        Set<String> ourTeamPuuids = myTeam != null ? teamPuuids.getOrDefault(myTeam, Set.of()) : Set.of();

        return ValorantRoundScoreInput.of(
                playerRoundStats,
                myKills,
                myAssists,
                damageToEliminated != null ? damageToEliminated : Map.of(),
                myDamagePerKill != null ? myDamagePerKill : Map.of(),
                ourLoadout,
                theirLoadout,
                myDeath,
                allKills,
                ourTeamPuuids
        );
    }
}
