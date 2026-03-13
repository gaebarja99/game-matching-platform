package com.gamematcher.mapper;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.ValorantMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import com.gamematcher.entity.match.valorant.ValorantKillAssistant;
import com.gamematcher.entity.match.valorant.ValorantKillEvent;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;

import com.gamematcher.service.ai.score.ValorantScoreService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Valorant 매치 → 플레이어별 매치 스탯 DTO 변환.
 * AI 분석용 매치 전체 지표 (KDA, KAST, ADR, 멀티킬 등).
 * ValorantScoreService로 라운드별 roundContributionScore를 채운다.
 */
@Component
public class ValorantMatchStatsMapper {

    private static final String GAME_VALORANT = "VALORANT";

    private final ValorantRoundStatsMapper roundStatsMapper;
    private final ValorantScoreService valorantScoreService;

    public ValorantMatchStatsMapper(ValorantRoundStatsMapper roundStatsMapper,
                                    ValorantScoreService valorantScoreService) {
        this.roundStatsMapper = roundStatsMapper;
        this.valorantScoreService = valorantScoreService;
    }

    /**
     * 매치 전체에서 플레이어별 매치 스탯 DTO 목록 생성.
     */
    public List<ValorantMatchStatsDTO> toMatchStatsDtos(ValorantMatch match) {
        if (match == null || match.getPlayers() == null) {
            return List.of();
        }

        List<ValorantRoundStatsDTO> roundStats = roundStatsMapper.toRoundStatsDtos(match);
        Map<String, List<ValorantRoundStatsDTO>> roundStatsByPuuid = roundStats.stream()
                .filter(rs -> rs.getPlayerPuuid() != null)
                .collect(Collectors.groupingBy(ValorantRoundStatsDTO::getPlayerPuuid));

        int roundsPlayed = nullToZero(match.getRoundsPlayed());
        if (roundsPlayed == 0 && match.getRounds() != null) {
            roundsPlayed = match.getRounds().size();
        }
        final int finalRoundsPlayed = roundsPlayed;

        return match.getPlayers().stream()
                .map(player -> toMatchStatsDto(player, roundStatsByPuuid.get(player.getPuuid()), finalRoundsPlayed, match))
                .toList();
    }

    /**
     * 매치 전체에서 플레이어별 ValorantPlayerMatchStatsDTO 목록 생성.
     * (매치 식별자, 플레이어 정보, 에이전트, matchStats, roundStats 포함)
     */
    public List<ValorantPlayerMatchStatsDTO> toPlayerMatchStatsDtos(ValorantMatch match) {
        if (match == null || match.getPlayers() == null) {
            return List.of();
        }

        List<ValorantMatchStatsDTO> matchStatsList = toMatchStatsDtos(match);
        List<ValorantRoundStatsDTO> roundStats = roundStatsMapper.toRoundStatsDtos(match);
        Map<String, List<ValorantRoundStatsDTO>> roundStatsByPuuid = roundStats.stream()
                .filter(rs -> rs.getPlayerPuuid() != null)
                .collect(Collectors.groupingBy(ValorantRoundStatsDTO::getPlayerPuuid));

        List<ValorantPlayerMatchStatsDTO> result = new ArrayList<>();
        for (int i = 0; i < match.getPlayers().size(); i++) {
            ValorantMatchPlayer player = match.getPlayers().get(i);
            ValorantMatchStatsDTO matchStats = matchStatsList.get(i);

            ValorantPlayerMatchStatsDTO dto = new ValorantPlayerMatchStatsDTO();
            dto.setMatchId(match.getMatchId());
            dto.setPlayerPuuid(player.getPuuid());
            dto.setPlayerDisplayName(formatPlayerDisplayName(player.getName(), player.getTag()));
            dto.setPlayerTeam(player.getTeam());
            dto.setAgent(player.getAgent());
            dto.setMatchStats(matchStats);
            dto.setRoundStats(roundStatsByPuuid.getOrDefault(player.getPuuid(), List.of()));
            // 라운드별 승리 기여도 점수 설정 + 매치 평균 기여도 점수 반영
            int matchAverageContributionScore = valorantScoreService.calculate(dto, match);
            matchStats.setMatchAverageContributionScore(matchAverageContributionScore);
            result.add(dto);
        }
        return result;
    }

    private String formatPlayerDisplayName(String name, String tag) {
        if (name == null && tag == null) return null;
        if (tag == null || tag.isBlank()) return name;
        return (name != null ? name : "") + "#" + tag;
    }

    private ValorantMatchStatsDTO toMatchStatsDto(
            ValorantMatchPlayer player,
            List<ValorantRoundStatsDTO> playerRoundStats,
            int roundsPlayed,
            ValorantMatch match
    ) {
        ValorantMatchStatsDTO dto = new ValorantMatchStatsDTO();
        dto.setGame(GAME_VALORANT);

        // BaseStatsDTO
        dto.setKills(nullToZero(player.getKills()));
        dto.setDeaths(nullToZero(player.getDeaths()));
        dto.setAssists(nullToZero(player.getAssists()));
        dto.setResult(player.isWin() ? MatchResult.VICTORY : MatchResult.DEFEAT);

        // 라운드
        dto.setRoundsPlayed(roundsPlayed);
        dto.setRoundsWon(getRoundsWonForPlayer(player.getTeam(), match));

        // 타격
        int head = nullToZero(player.getHeadshots());
        int body = nullToZero(player.getBodyshots());
        int leg = nullToZero(player.getLegshots());
        dto.setTotalShots(head + body + leg);
        dto.setHeadShots(head);
        dto.setBodyShots(body);
        dto.setLegShots(leg);

        // 파생: kd, shot rates
        int kills = dto.getKills();
        int deaths = dto.getDeaths();
        int totalShots = dto.getTotalShots();
        dto.setKd(deaths > 0 ? (double) kills / deaths : kills);
        dto.setHeadShotRate(totalShots > 0 ? (head * 100.0 / totalShots) : 0);
        dto.setBodyShotRate(totalShots > 0 ? (body * 100.0 / totalShots) : 0);
        dto.setLegShotRate(totalShots > 0 ? (leg * 100.0 / totalShots) : 0);

        // 라운드 스탯 집계
        if (playerRoundStats != null && !playerRoundStats.isEmpty()) {
            dto.setFirstBloods((int) playerRoundStats.stream().filter(ValorantRoundStatsDTO::isFirstKill).count());
            dto.setFirstDeaths((int) playerRoundStats.stream().filter(ValorantRoundStatsDTO::isFirstDeath).count());

            int doubleKill = 0, tripleKill = 0, quadraKill = 0, pentaKill = 0, overKill = 0;
            int totalDamage = 0;
            int kastRounds = 0;

            List<ValorantKillEvent> allKills = match.getKillEvents() != null ? match.getKillEvents() : List.of();
            String playerPuuid = player.getPuuid();
            String playerTeam = player.getTeam();

            for (ValorantRoundStatsDTO rs : playerRoundStats) {
                int k = rs.getKills();
                if (k >= 6) overKill++;
                else if (k == 5) pentaKill++;
                else if (k == 4) quadraKill++;
                else if (k == 3) tripleKill++;
                else if (k == 2) doubleKill++;

                totalDamage += rs.getDamage();

                boolean k_ = k > 0;
                boolean s_ = !rs.isDied();
                List<ValorantKillEvent> roundKills = getKillsInRound(rs.getRoundIndex(), allKills);
                boolean a_ = hasAssistInRound(playerPuuid, roundKills);
                boolean t_ = isTradedInRound(playerPuuid, playerTeam, roundKills);
                if (k_ || a_ || s_ || t_) kastRounds++;
            }

            dto.setDoubleKill(doubleKill);
            dto.setTripleKill(tripleKill);
            dto.setQuadraKill(quadraKill);
            dto.setPentaKill(pentaKill);
            dto.setOverKill(overKill);
            dto.setMultiKill(doubleKill + tripleKill + quadraKill + pentaKill + overKill);

            dto.setAdr(roundsPlayed > 0 ? (double) totalDamage / roundsPlayed : 0);
            dto.setKast(roundsPlayed > 0 ? (kastRounds * 100.0 / roundsPlayed) : 0);
        } else {
            dto.setFirstBloods(0);
            dto.setFirstDeaths(0);
            dto.setDoubleKill(0);
            dto.setTripleKill(0);
            dto.setQuadraKill(0);
            dto.setPentaKill(0);
            dto.setOverKill(0);
            dto.setMultiKill(0);
            dto.setAdr(0);
            dto.setKast(0);
        }

        // avgDamageDifference: (damageMade - damageReceived) / roundsPlayed
        int damageMade = nullToZero(player.getDamageMade());
        int damageReceived = nullToZero(player.getDamageReceived());
        dto.setAvgDamageDifference(roundsPlayed > 0
                ? (double) (damageMade - damageReceived) / roundsPlayed
                : 0);

        return dto;
    }

    private static final int TRADED_WINDOW_MS = 5000;

    /** API round는 0-based. roundIndex와 동일한 킬만 포함 */
    private List<ValorantKillEvent> getKillsInRound(int roundIndex, List<ValorantKillEvent> allKills) {
        if (allKills == null) return List.of();
        final int rIdx = roundIndex;
        return allKills.stream()
                .filter(ke -> {
                    Integer rn = ke.getRoundNumber();
                    return rn != null && rn == rIdx;
                })
                .toList();
    }

    private boolean hasAssistInRound(String playerPuuid, List<ValorantKillEvent> roundKills) {
        if (playerPuuid == null || roundKills == null) return false;
        return roundKills.stream().anyMatch(ke -> {
            if (ke.getAssistants() == null) return false;
            return ke.getAssistants().stream()
                    .map(ValorantKillAssistant::getAssistantPuuid)
                    .anyMatch(playerPuuid::equals);
        });
    }

    /**
     * Traded: 해당 라운드에서 플레이어가 죽은 적이 있고,
     * 팀원이 5초 이내에 킬러를 제거한 경우.
     * 세이지 부활로 한 라운드에 여러 번 죽을 수 있으므로, 모든 데스를 검사하여
     * 하나라도 traded면 해당 라운드는 KAST T로 인정.
     */
    private boolean isTradedInRound(String playerPuuid, String playerTeam,
                                   List<ValorantKillEvent> roundKills) {
        if (playerPuuid == null || playerTeam == null || roundKills == null) return false;

        List<ValorantKillEvent> myDeaths = roundKills.stream()
                .filter(ke -> playerPuuid.equals(ke.getVictimPuuid()))
                .toList();
        if (myDeaths.isEmpty()) return false;

        for (ValorantKillEvent myDeath : myDeaths) {
            String killerPuuid = myDeath.getKillerPuuid();
            Integer myDeathTime = myDeath.getKillTimeInRound();
            if (killerPuuid == null || myDeathTime == null) continue;

            boolean traded = roundKills.stream()
                    .filter(ke -> killerPuuid.equals(ke.getVictimPuuid()) && playerTeam.equals(ke.getKillerTeam()))
                    .anyMatch(avenge -> {
                        Integer avengeTime = avenge.getKillTimeInRound();
                        return avengeTime != null && avengeTime >= myDeathTime
                                && (avengeTime - myDeathTime) <= TRADED_WINDOW_MS;
                    });
            if (traded) return true;
        }
        return false;
    }

    private int getRoundsWonForPlayer(String team, ValorantMatch match) {
        if (match == null || team == null) return 0;

        if ("Red".equalsIgnoreCase(team)) {
            return nullToZero(match.getRedRoundsWon());
        }
        if ("Blue".equalsIgnoreCase(team)) {
            return nullToZero(match.getBlueRoundsWon());
        }
        return 0;
    }

    private int nullToZero(Integer v) {
        return v != null ? v : 0;
    }
}
