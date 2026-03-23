package com.gamematcher.service.ai.score;

import com.gamematcher.dto.valorant.ValorantAssistContext;
import com.gamematcher.dto.valorant.ValorantKillContext;
import com.gamematcher.entity.match.valorant.ValorantKillAssistant;
import com.gamematcher.entity.match.valorant.ValorantKillEvent;
import com.gamematcher.entity.match.valorant.ValorantMatchRound;
import com.gamematcher.entity.match.valorant.ValorantMatchRoundPlayer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.gamematcher.constant.ai.evaluation.ValorantRoundScoreConstants.ALIVE_CAP;

/**
 * 라운드별 킬 이벤트에서 ValorantKillContext, ValorantAssistContext 추출.
 * 킬 직전 생존 수·팀별 로드아웃을 반영. 세이지 부활 시 alive 상한 적용.
 */
@Component
public class KillContextExtractor {

    /**
     * 라운드 킬 이벤트로부터 ValorantKillContext, ValorantAssistContext를 플레이어별로 추출.
     *
     * @param round      라운드 (playerStats로 팀·로드아웃 매핑)
     * @param roundKills 해당 라운드 킬 이벤트 (kill_time_in_round 오름차순 정렬 권장)
     * @return 추출 결과 (killsByKiller, assistsByAssistant)
     */
    public ExtractResult extract(ValorantMatchRound round, List<ValorantKillEvent> roundKills) {
        Map<String, List<ValorantKillContext>> killsByKiller = new HashMap<>();
        Map<String, List<ValorantAssistContext>> assistsByAssistant = new HashMap<>();

        if (round == null || round.getPlayerStats() == null || roundKills == null || roundKills.isEmpty()) {
            return new ExtractResult(killsByKiller, assistsByAssistant);
        }

        // puuid -> team
        Map<String, String> puuidToTeam = new HashMap<>();
        for (ValorantMatchRoundPlayer rp : round.getPlayerStats()) {
            if (rp.getPlayerPuuid() != null && rp.getPlayerTeam() != null) {
                puuidToTeam.put(rp.getPlayerPuuid(), rp.getPlayerTeam());
            }
        }

        // team -> loadout sum
        Map<String, Integer> teamLoadout = new HashMap<>();
        for (ValorantMatchRoundPlayer rp : round.getPlayerStats()) {
            String team = rp.getPlayerTeam();
            if (team == null) continue;
            int loadout = rp.getLoadoutValue() != null ? rp.getLoadoutValue() : 0;
            teamLoadout.merge(team, loadout, Integer::sum);
        }

        // team -> alive count (초기 5)
        Map<String, Integer> teamAlive = new HashMap<>();
        for (String team : teamLoadout.keySet()) {
            teamAlive.put(team, ALIVE_CAP);
        }

        // kill_time_in_round 오름차순
        List<ValorantKillEvent> sorted = new ArrayList<>(roundKills);
        sorted.sort(Comparator.comparingInt(k -> k.getKillTimeInRound() != null ? k.getKillTimeInRound() : 0));

        for (ValorantKillEvent kill : sorted) {
            String killerPuuid = kill.getKillerPuuid();
            String victimPuuid = kill.getVictimPuuid();
            String killerTeam = kill.getKillerTeam();
            String victimTeam = kill.getVictimTeam();

            if (killerPuuid == null || victimPuuid == null || killerTeam == null || victimTeam == null) {
                continue;
            }

            int ourAlive = Math.min(teamAlive.getOrDefault(killerTeam, ALIVE_CAP), ALIVE_CAP);
            int theirAlive = Math.min(teamAlive.getOrDefault(victimTeam, ALIVE_CAP), ALIVE_CAP);

            int ourLoadout = teamLoadout.getOrDefault(killerTeam, 0);
            int theirLoadout = teamLoadout.getOrDefault(victimTeam, 0);

            int killTime = kill.getKillTimeInRound() != null ? kill.getKillTimeInRound() : 0;

            ValorantKillContext kc = new ValorantKillContext(
                    killerPuuid, victimPuuid,
                    ourAlive, theirAlive,
                    ourLoadout, theirLoadout,
                    killTime
            );

            killsByKiller.computeIfAbsent(killerPuuid, k -> new ArrayList<>()).add(kc);

            // 어시스트
            if (kill.getAssistants() != null) {
                for (ValorantKillAssistant asst : kill.getAssistants()) {
                    String assistantPuuid = asst.getAssistantPuuid();
                    if (assistantPuuid != null) {
                        assistsByAssistant.computeIfAbsent(assistantPuuid, a -> new ArrayList<>())
                                .add(new ValorantAssistContext(kc));
                    }
                }
            }

            // victim 팀 alive 감소
            teamAlive.merge(victimTeam, -1, Integer::sum);
            teamAlive.put(victimTeam, Math.max(0, teamAlive.get(victimTeam)));
        }

        return new ExtractResult(killsByKiller, assistsByAssistant);
    }

    /** 추출 결과: 킬러별 킬 컨텍스트, 어시스트별 어시스트 컨텍스트 */
    public record ExtractResult(
            Map<String, List<ValorantKillContext>> killsByKiller,
            Map<String, List<ValorantAssistContext>> assistsByAssistant
    ) {}
}
