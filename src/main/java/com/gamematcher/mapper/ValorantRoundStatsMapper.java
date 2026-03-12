package com.gamematcher.mapper;

import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import com.gamematcher.entity.match.valorant.ValorantKillEvent;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.entity.match.valorant.ValorantMatchRound;
import com.gamematcher.entity.match.valorant.ValorantMatchRoundPlayer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Valorant 매치 → 라운드별 플레이어 스탯 DTO 변환.
 * 매치 전체 스탯과 분리 구조로 AI 분석에 사용.
 */
@Component
public class ValorantRoundStatsMapper {

    /**
     * 매치 전체에서 라운드×플레이어 단위 스탯 목록 생성.
     */
    public List<ValorantRoundStatsDTO> toRoundStatsDtos(ValorantMatch match) {
        if (match == null || match.getRounds() == null) {
            return List.of();
        }

        List<ValorantRoundStatsDTO> result = new ArrayList<>();
        for (ValorantMatchRound round : match.getRounds()) {
            result.addAll(toRoundStatsDtosForRound(round, match.getKillEvents()));
        }
        return result;
    }

    private List<ValorantRoundStatsDTO> toRoundStatsDtosForRound(
            ValorantMatchRound round,
            List<ValorantKillEvent> killEvents
    ) {
        if (round == null || round.getPlayerStats() == null) {
            return List.of();
        }

        int roundIndex = round.getRoundIndex() != null ? round.getRoundIndex() : -1;
        String winningTeam = round.getWinningTeam();

        // 해당 라운드의 첫 킬 이벤트 (FB/FD 판정용)
        Optional<ValorantKillEvent> firstKill = findFirstKillInRound(roundIndex, killEvents);
        String firstBloodKiller = firstKill.map(ValorantKillEvent::getKillerPuuid).orElse(null);
        String firstDeathVictim = firstKill.map(ValorantKillEvent::getVictimPuuid).orElse(null);

        List<ValorantRoundStatsDTO> result = new ArrayList<>();
        for (ValorantMatchRoundPlayer rp : round.getPlayerStats()) {
            result.add(toRoundStatsDto(rp, roundIndex, winningTeam, firstBloodKiller, firstDeathVictim));
        }
        return result;
    }

    private ValorantRoundStatsDTO toRoundStatsDto(
            ValorantMatchRoundPlayer rp,
            int roundIndex,
            String winningTeam,
            String firstBloodKiller,
            String firstDeathVictim
    ) {
        ValorantRoundStatsDTO dto = new ValorantRoundStatsDTO();
        dto.setRoundIndex(roundIndex);
        dto.setPlayerPuuid(rp.getPlayerPuuid());
        dto.setPlayerDisplayName(rp.getPlayerDisplayName());
        dto.setPlayerTeam(rp.getPlayerTeam());

        dto.setDamage(nullToZero(rp.getDamage()));
        dto.setHeadshots(nullToZero(rp.getHeadshots()));
        dto.setBodyshots(nullToZero(rp.getBodyshots()));
        dto.setLegshots(nullToZero(rp.getLegshots()));
        dto.setKills(nullToZero(rp.getKills()));
        dto.setScore(nullToZero(rp.getScore()));

        dto.setLoadoutValue(rp.getLoadoutValue());
        dto.setEconomyRemaining(rp.getEconomyRemaining());
        dto.setEconomySpent(rp.getEconomySpent());

        dto.setAbilityXCasts(nullToZero(rp.getAbilityXCasts()));
        dto.setAbilityECasts(nullToZero(rp.getAbilityECasts()));
        dto.setAbilityQCasts(nullToZero(rp.getAbilityQCasts()));
        dto.setAbilityCCasts(nullToZero(rp.getAbilityCCasts()));

        dto.setWasAfk(Boolean.TRUE.equals(rp.getWasAfk()));
        dto.setWasPenalized(Boolean.TRUE.equals(rp.getWasPenalized()));
        dto.setStayedInSpawn(Boolean.TRUE.equals(rp.getStayedInSpawn()));

        dto.setGotFirstBlood(rp.getPlayerPuuid() != null && rp.getPlayerPuuid().equals(firstBloodKiller));
        dto.setWasFirstDeath(rp.getPlayerPuuid() != null && rp.getPlayerPuuid().equals(firstDeathVictim));

        dto.setRoundWon(winningTeam != null && winningTeam.equals(rp.getPlayerTeam()));

        return dto;
    }

    /**
     * Valorant API: roundNumber는 1-based, roundIndex는 0-based.
     * roundIndex 0 → roundNumber 1, roundIndex 1 → roundNumber 2
     */
    private Optional<ValorantKillEvent> findFirstKillInRound(int roundIndex, List<ValorantKillEvent> killEvents) {
        if (killEvents == null) return Optional.empty();
        int apiRoundNumber = roundIndex + 1;
        return killEvents.stream()
                .filter(ke -> apiRoundNumber == (ke.getRoundNumber() != null ? ke.getRoundNumber() : -1))
                .min((a, b) -> Integer.compare(
                        nullToMax(a.getKillTimeInRound()),
                        nullToMax(b.getKillTimeInRound())
                ));
    }

    private int nullToZero(Integer v) {
        return v != null ? v : 0;
    }

    private int nullToMax(Integer v) {
        return v != null ? v : Integer.MAX_VALUE;
    }
}
