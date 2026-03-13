package com.gamematcher.dto.ai.evaluation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ValorantMatchStatsDTO extends BaseStatsDTO {

    /** 라운드 */
    private int roundsPlayed;  // 플레이한 총 라운드 수
    private int roundsWon;     // 팀이 이긴 라운드 수

    /** KDA 지표 */
    private int kills;           // 킬 수
    private int deaths;          // 데스 수
    private int assists;         // 어시스트 수

    /** 타격 (샷 수) */
    private int totalShots;  // 총 피격 수 (head+body+leg)
    private int headShots;   // 헤드샷 수
    private int bodyShots;   // 몸 샷 수
    private int legShots;    // 다리 샷 수

    /** 비율 (계산값) */
    private double kd;               // KD = kills/deaths
    private double headShotRate;     // 헤드샷 비율 (0~100%) → headShots/totalShots
    private double bodyShotRate;     // 몸샷 비율 (0~100%) → bodyShots/totalShots
    private double legShotRate;      // 다리샷 비율 (0~100%) → legShots/totalShots
    // private double avgReversalIndex; // 역전 지수
    // Index = ((Enemy_Loadout + 1000) / (Player_Loadout + 1000)) * (Damage_Dealt / (Damage_Received + 50))

    /** 교전·팀플레이 지표 */
    private double kast;                // KAST: 킬/어시/생존/트레이드 라운드 비율(0~100%)
    private double adr;                 // ADR: 라운드당 평균 데미지
    private double avgDamageDifference; // DDΔ: 평균 피해량 격차 (가한-받은)/라운드

    /** 매치 평균 기여도 점수 (라운드별 roundContributionScore의 평균, 100 기준) */
    private int matchAverageContributionScore;

    /** 엔트리 지표 */
    private int firstBloods;  // 첫 킬을 올린 라운드 수
    private int firstDeaths;  // 첫 사망이 본인인 라운드 수

    /** 킬스트릭 지표 */
    private int multiKill;    // 멀티킬 수
    private int doubleKill;   // 더블킬 수
    private int tripleKill;   // 트리플킬 수
    private int quadraKill;   // 쿼드라킬 수
    private int pentaKill;    // 펜타킬 수
    private int overKill;     // 오버킬 수 (6킬 이상)
}
