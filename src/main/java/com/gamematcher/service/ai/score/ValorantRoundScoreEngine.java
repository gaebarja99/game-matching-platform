package com.gamematcher.service.ai.score;

import com.gamematcher.constant.ai.evaluation.ValorantRoundScoreConstants;
import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import com.gamematcher.dto.valorant.ValorantAssistContext;
import com.gamematcher.dto.valorant.ValorantKillContext;
import com.gamematcher.dto.valorant.ValorantRoundScoreInput;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 발로란트 라운드별 승리 기여도 점수 엔진.
 * 킬 가중치(경제 역전·상황 변화), 어시스트(데미지 비례), 데미지 기여, 클러치, 사망 패널티를 반영.
 */
@Component
public class ValorantRoundScoreEngine {

    /**
     * 플레이어 1명의 라운드 1개 점수 계산.
     *
     * @param input 라운드 스탯, 킬/어시스트 컨텍스트, 데미지 기여
     * @return 라운드 점수 (100 기준, 가감)
     */
    public int calculateRound(ValorantRoundScoreInput input) {
        if (input == null || input.playerRoundStats() == null) {
            return ValorantRoundScoreConstants.BASE_SCORE;
        }

        ValorantRoundStatsDTO stats = input.playerRoundStats();
        int score = ValorantRoundScoreConstants.BASE_SCORE;

        // Afk / Penalized (Lombok @Getter: boolean → isXxx())
        if (stats.isAfk() || stats.isPenalized()) {
            return score + ValorantRoundScoreConstants.AFK_PENALTY; // 100 - 100 = 0
        }

        // 킬 가중 점수
        score += calcKillScores(input);

        // 어시스트 점수 (데미지 비례)
        score += calcAssistScores(input);

        // 제거 기여 데미지 (어시스트자 제외, 상호 배타적)
        score += calcDamageContributionScores(input);

        // 클러치 판별
        if (stats.isRoundWon() && hadClutch(input)) {
            score += ValorantRoundScoreConstants.CLUTCH_BONUS;
        }

        // roundWon 보너스
        if (stats.isRoundWon()) {
            score += ValorantRoundScoreConstants.ROUND_WIN_BONUS;
        }

        // 사망 패널티
        score += calcDeathPenalty(input);

        return score;
    }

    private int calcKillScores(ValorantRoundScoreInput input) {
        int sum = 0;
        var myKills = input.myKillsInRound();
        var myDamagePerKill = input.myDamagePerKill();

        for (ValorantKillContext kc : myKills) {
            double economyMult = calcEconomyMultiplier(kc.ourTeamLoadout(), kc.theirTeamLoadout());
            double situationMult = getSituationMultiplier(kc.ourAliveAtKill(), kc.theirAliveAtKill());
            double damageRatio = getKillDamageRatio(myDamagePerKill.getOrDefault(kc.victimPuuid(), 0));
            double killValue = ValorantRoundScoreConstants.KILL_BASE * economyMult * situationMult * damageRatio;
            sum += (int) Math.round(killValue);
        }
        return sum;
    }

    private double calcEconomyMultiplier(int ourLoadout, int theirLoadout) {
        int our = Math.max(ourLoadout, ValorantRoundScoreConstants.ECONOMY_FLOOR);
        double raw = (double) theirLoadout / our;
        return Math.max(ValorantRoundScoreConstants.ECONOMY_MULTIPLIER_MIN,
                Math.min(ValorantRoundScoreConstants.ECONOMY_MULTIPLIER_CAP, raw));
    }

    private double getSituationMultiplier(int ourAlive, int theirAlive) {
        int our = Math.min(ourAlive, ValorantRoundScoreConstants.ALIVE_CAP);
        int their = Math.min(theirAlive, ValorantRoundScoreConstants.ALIVE_CAP);
        int ourAfter = our;
        int theirAfter = Math.max(0, their - 1);

        // 5v5 -> 5v4 (FB)
        if (our == 5 && their == 5) return ValorantRoundScoreConstants.SITUATION_FIRST_KILL;
        // 열세->동률 (ours < theirs, after kill ours == their)
        if (our < their && ourAfter == theirAfter) return ValorantRoundScoreConstants.SITUATION_EQUALIZE;
        // 동률->우세 (ours == theirs, after kill ours > their)
        if (our == their && ourAfter > theirAfter) return ValorantRoundScoreConstants.SITUATION_TAKE_LEAD;
        // 클린업 (3v1, 4v1, 5v1 등)
        if (our >= 3 && their <= 1) return ValorantRoundScoreConstants.SITUATION_CLEANUP;
        // 열세->여전히 열세
        if (our < their && ourAfter < theirAfter) return ValorantRoundScoreConstants.SITUATION_STILL_BEHIND;

        return ValorantRoundScoreConstants.SITUATION_DEFAULT;
    }

    private double getKillDamageRatio(int myDamage) {
        return Math.max(ValorantRoundScoreConstants.KILL_DAMAGE_RATIO_MIN,
                Math.min(1.0, myDamage / 100.0));
    }

    private int calcAssistScores(ValorantRoundScoreInput input) {
        int sum = 0;
        var myAssists = input.myAssistsInRound();
        var damageToEliminated = input.damageToEliminated();

        for (ValorantAssistContext ac : myAssists) {
            ValorantKillContext kc = ac.killContext();
            double economyMult = calcEconomyMultiplier(kc.ourTeamLoadout(), kc.theirTeamLoadout());
            double situationMult = getSituationMultiplier(kc.ourAliveAtKill(), kc.theirAliveAtKill());
            double killValue = ValorantRoundScoreConstants.KILL_BASE * economyMult * situationMult;

            int myDamage = damageToEliminated.getOrDefault(kc.victimPuuid(), 0);
            double damageRatio = (myDamage > 0)
                    ? Math.min(ValorantRoundScoreConstants.ASSIST_DAMAGE_RATIO_CAP, myDamage / 100.0)
                    : 1.0; // 스킬 어시스트
            double assistScore = killValue * ValorantRoundScoreConstants.ASSIST_RATIO * damageRatio;
            sum += (int) Math.round(assistScore);
        }
        return sum;
    }

    private int calcDamageContributionScores(ValorantRoundScoreInput input) {
        // 어시스트 판정받은 victim 제외 (상호 배타적)
        Set<String> assistVictims = new HashSet<>();
        for (ValorantAssistContext ac : input.myAssistsInRound()) {
            assistVictims.add(ac.killContext().victimPuuid());
        }
        Set<String> myKillVictims = new HashSet<>();
        for (ValorantKillContext kc : input.myKillsInRound()) {
            myKillVictims.add(kc.victimPuuid());
        }

        int sum = 0;
        var damageToEliminated = input.damageToEliminated();
        for (Map.Entry<String, Integer> e : damageToEliminated.entrySet()) {
            String victim = e.getKey();
            int dmg = e.getValue();
            if (assistVictims.contains(victim) || myKillVictims.contains(victim)) continue;
            sum += (int) Math.round((dmg / 100.0) * ValorantRoundScoreConstants.DAMAGE_SCORE_PER_100);
        }
        return sum;
    }

    private boolean hadClutch(ValorantRoundScoreInput input) {
        for (ValorantKillContext kc : input.myKillsInRound()) {
            if (kc.ourAliveAtKill() == 1 && kc.theirAliveAtKill() >= 2) return true;
        }
        for (ValorantAssistContext ac : input.myAssistsInRound()) {
            var kc = ac.killContext();
            if (kc.ourAliveAtKill() == 1 && kc.theirAliveAtKill() >= 2) return true;
        }
        return false;
    }

    private int calcDeathPenalty(ValorantRoundScoreInput input) {
        ValorantKillContext myDeathContext = input.myDeathInRound();
        if (myDeathContext == null) return 0;

        // victim(나) 관점: our = 내 팀, their = 킬러 팀
        int ourAlive = myDeathContext.theirAliveAtKill();  // victim 팀 = 내 팀
        int theirAlive = myDeathContext.ourAliveAtKill(); // killer 팀 = 적 팀
        int myDeathTime = myDeathContext.killTimeInRound();
        String myKillerPuuid = myDeathContext.killerPuuid();

        // 트레이드: 4초 내 아군이 내 킬러 제거
        Set<String> ourTeam = input.ourTeamPuuids();
        boolean traded = false;
        for (ValorantKillContext kc : input.allKillsInRound()) {
            if (kc.victimPuuid().equals(myKillerPuuid)) {
                int diff = kc.killTimeInRound() - myDeathTime;
                if (diff >= 0 && diff <= ValorantRoundScoreConstants.TRADE_WINDOW_MS
                        && ourTeam.contains(kc.killerPuuid())) {
                    traded = true;
                }
                break;
            }
        }
        if (traded) return ValorantRoundScoreConstants.DEATH_PENALTY_TRADE; // 0

        // 5v5 단독 사망
        if (ourAlive == 5 && theirAlive == 5) return ValorantRoundScoreConstants.DEATH_PENALTY_5V5_LONE;

        // FirstDeath + 인원 불리 (2v4 등)
        ValorantRoundStatsDTO stats = input.playerRoundStats();
        if (stats.isFirstDeath() && ourAlive < theirAlive) return ValorantRoundScoreConstants.DEATH_PENALTY_FIRST_BLOOD_LOST;

        // 경제 불리 + 사망 (경제 역전 지수로 간단 판별)
        int ourLoadout = input.ourTeamLoadout();
        int theirLoadout = input.theirTeamLoadout();
        if (ourLoadout < theirLoadout * 0.7) return ValorantRoundScoreConstants.DEATH_PENALTY_ECO_DISADVANTAGE;

        return ValorantRoundScoreConstants.DEATH_PENALTY_DEFAULT;
    }
}
