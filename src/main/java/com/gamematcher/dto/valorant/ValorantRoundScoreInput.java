package com.gamematcher.dto.valorant;

import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 발로란트 플레이어 1명의 라운드 1개 점수 계산 입력.
 */
public record ValorantRoundScoreInput(
        ValorantRoundStatsDTO playerRoundStats,
        List<ValorantKillContext> myKillsInRound,
        List<ValorantAssistContext> myAssistsInRound,
        Map<String, Integer> damageToEliminated,
        Map<String, Integer> myDamagePerKill,
        int ourTeamLoadout,
        int theirTeamLoadout,
        /** 내가 사망한 킬의 컨텍스트 (victim=me). 없으면 null */
        ValorantKillContext myDeathInRound,
        /** 라운드 전체 킬 컨텍스트. 트레이드 판별용 */
        List<ValorantKillContext> allKillsInRound,
        /** 우리팀 puuid 목록. 트레이드 판별용 */
        Set<String> ourTeamPuuids
) {
    /** 빈 맵으로 null 안전하게 사용 */
    public static ValorantRoundScoreInput of(
            ValorantRoundStatsDTO playerRoundStats,
            List<ValorantKillContext> myKillsInRound,
            List<ValorantAssistContext> myAssistsInRound,
            Map<String, Integer> damageToEliminated,
            Map<String, Integer> myDamagePerKill,
            int ourTeamLoadout,
            int theirTeamLoadout,
            ValorantKillContext myDeathInRound,
            List<ValorantKillContext> allKillsInRound,
            Set<String> ourTeamPuuids
    ) {
        return new ValorantRoundScoreInput(
                playerRoundStats,
                myKillsInRound != null ? myKillsInRound : List.of(),
                myAssistsInRound != null ? myAssistsInRound : List.of(),
                damageToEliminated != null ? damageToEliminated : Map.of(),
                myDamagePerKill != null ? myDamagePerKill : Map.of(),
                ourTeamLoadout,
                theirTeamLoadout,
                myDeathInRound,
                allKillsInRound != null ? allKillsInRound : List.of(),
                ourTeamPuuids != null ? ourTeamPuuids : Set.of()
        );
    }
}
