package com.gamematcher.mapper;

import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import com.gamematcher.entity.match.valorant.ValorantKillEvent;
import com.gamematcher.entity.match.valorant.ValorantKillEventPlayerLocation;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.entity.match.valorant.ValorantMatchRound;
import com.gamematcher.entity.match.valorant.ValorantMatchRoundPlayer;
import com.gamematcher.entity.match.valorant.ValorantRoundPlayerDamageEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valorant 매치 → 라운드별 플레이어 스탯 DTO 변환.
 * 매치 전체 스탯과 분리 구조로 AI 분석에 사용.
 */
@Component
public class ValorantRoundStatsMapper {

    /**
     * 단일 라운드에서 플레이어별 스탯 DTO 목록 생성.
     * killEvents가 null이면 FirstKill/FirstDeath는 false.
     */
    public List<ValorantRoundStatsDTO> toRoundStatsDtos(
            ValorantMatchRound round,
            List<ValorantKillEvent> killEvents
    ) {
        return toRoundStatsDtosForRound(round, killEvents);
    }

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

        // 해당 라운드 킬 이벤트 (victim 목록·killer 추출용)
        List<ValorantKillEvent> roundKills = getKillsInRound(roundIndex, killEvents);

        List<ValorantRoundStatsDTO> result = new ArrayList<>();
        for (ValorantMatchRoundPlayer rp : round.getPlayerStats()) {
            result.add(toRoundStatsDto(rp, roundIndex, winningTeam, firstBloodKiller, firstDeathVictim, roundKills));
        }
        return result;
    }

    /** API round는 0-based. roundIndex와 동일한 킬만 포함 (rn+1 제외) */
    private List<ValorantKillEvent> getKillsInRound(int roundIndex, List<ValorantKillEvent> killEvents) {
        if (killEvents == null) return List.of();
        final int rIdx = roundIndex;
        return killEvents.stream()
                .filter(ke -> {
                    Integer rn = ke.getRoundNumber();
                    return rn != null && rn == rIdx;
                })
                .toList();
    }

    private ValorantRoundStatsDTO toRoundStatsDto(
            ValorantMatchRoundPlayer rp,
            int roundIndex,
            String winningTeam,
            String firstBloodKiller,
            String firstDeathVictim,
            List<ValorantKillEvent> roundKills
    ) {
        int head = nullToZero(rp.getHeadshots());
        int body = nullToZero(rp.getBodyshots());
        int leg = nullToZero(rp.getLegshots());

        ValorantRoundStatsDTO dto = new ValorantRoundStatsDTO();
        dto.setRoundIndex(roundIndex);
        dto.setPlayerPuuid(rp.getPlayerPuuid());

        dto.setDamage(nullToZero(rp.getDamage()));
        dto.setTotalShots(head + body + leg);
        dto.setHeadShots(head);
        dto.setBodyShots(body);
        dto.setLegShots(leg);
        dto.setKills(nullToZero(rp.getKills()));
        dto.setScore(nullToZero(rp.getScore()));

        dto.setLoadoutValue(rp.getLoadoutValue());
        dto.setEconomyRemaining(rp.getEconomyRemaining());
        dto.setEconomySpent(rp.getEconomySpent());

        dto.setSkillXCasts(nullToZero(rp.getAbilityXCasts()));
        dto.setSkillECasts(nullToZero(rp.getAbilityECasts()));
        dto.setSkillQCasts(nullToZero(rp.getAbilityQCasts()));
        dto.setSkillCCasts(nullToZero(rp.getAbilityCCasts()));

        dto.setAfk(Boolean.TRUE.equals(rp.getWasAfk()));
        dto.setPenalized(Boolean.TRUE.equals(rp.getWasPenalized()));
        dto.setStayedInSpawn(Boolean.TRUE.equals(rp.getStayedInSpawn()));

        dto.setFirstKill(rp.getPlayerPuuid() != null && rp.getPlayerPuuid().equals(firstBloodKiller));
        dto.setFirstDeath(rp.getPlayerPuuid() != null && rp.getPlayerPuuid().equals(firstDeathVictim));

        dto.setRoundWon(winningTeam != null && winningTeam.equals(rp.getPlayerTeam()));

        // 라운드 내 사망 여부: 마지막 킬의 생존자 목록에 없으면 사망. 부활(세이지) 고려.
        // survivor 데이터 없으면 victim 여부로 폴백.
        Optional<Boolean> survived = getSurvivedFromLastKill(rp.getPlayerPuuid(), roundKills);
        boolean died = survived.isPresent()
                ? !survived.get()
                : roundKills.stream().anyMatch(k -> rp.getPlayerPuuid() != null && rp.getPlayerPuuid().equals(k.getVictimPuuid()));
        dto.setDied(died);

        // damageToEliminated, myDamagePerKill (라운드 점수 계산용)
        Set<String> victims = roundKills.stream()
                .map(ValorantKillEvent::getVictimPuuid)
                .filter(v -> v != null)
                .collect(Collectors.toSet());

        Map<String, Integer> damageToEliminated = new HashMap<>();
        Map<String, Integer> myDamagePerKill = new HashMap<>();
        if (rp.getDamageEvents() != null) {
            for (ValorantRoundPlayerDamageEvent de : rp.getDamageEvents()) {
                String receiver = de.getReceiverPuuid();
                if (receiver == null || !victims.contains(receiver)) continue;
                int dmg = nullToZero(de.getDamage());
                damageToEliminated.merge(receiver, dmg, Integer::sum);
                // 내가 킬한 victim이면 myDamagePerKill에도
                boolean iKilled = roundKills.stream()
                        .anyMatch(k -> rp.getPlayerPuuid().equals(k.getKillerPuuid()) && receiver.equals(k.getVictimPuuid()));
                if (iKilled) {
                    myDamagePerKill.merge(receiver, dmg, Integer::sum);
                }
            }
        }
        dto.setDamageToEliminated(damageToEliminated);
        dto.setMyDamagePerKill(myDamagePerKill);

        return dto;
    }

    /**
     * 마지막 킬의 player_locations_on_kill(생존자 목록)에서 플레이어 포함 여부.
     * 부활 고려: 생존자 목록에 있으면 survived. 없으면 died. 데이터 없으면 empty(폴백).
     */
    private Optional<Boolean> getSurvivedFromLastKill(String playerPuuid, List<ValorantKillEvent> roundKills) {
        if (playerPuuid == null || roundKills == null || roundKills.isEmpty()) return Optional.empty();
        Optional<ValorantKillEvent> lastKill = roundKills.stream()
                .max((a, b) -> Integer.compare(
                        nullToMax(a.getKillTimeInRound()),
                        nullToMax(b.getKillTimeInRound())));
        if (lastKill.isEmpty()) return Optional.empty();
        List<ValorantKillEventPlayerLocation> locs = lastKill.get().getPlayerLocations();
        if (locs == null || locs.isEmpty()) return Optional.empty();
        Set<String> survivors = locs.stream()
                .map(ValorantKillEventPlayerLocation::getPlayerPuuid)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toSet());
        return Optional.of(survivors.contains(playerPuuid));
    }

    /**
     * 해당 라운드의 가장 빠른 킬 이벤트 (First Blood / First Death 판정용).
     * API round는 0-based로 roundIndex와 동일. 동일 라운드 킬만 포함.
     */
    private Optional<ValorantKillEvent> findFirstKillInRound(int roundIndex, List<ValorantKillEvent> killEvents) {
        if (killEvents == null) return Optional.empty();
        final int rIdx = roundIndex;
        return killEvents.stream()
                .filter(ke -> {
                    Integer rn = ke.getRoundNumber();
                    return rn != null && rn == rIdx;
                })
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
