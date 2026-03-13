package com.gamematcher.dto.ai.evaluation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Valorant 라운드별 플레이어 스탯 (라운드×플레이어 단위).
 * AI 분석 시 매치 전체 스탯({@link ValorantMatchStatsDTO})과 분리하여 전달.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ValorantRoundStatsDTO {

    /** 라운드 인덱스 */
    private int roundIndex;
    private String playerPuuid;
    private boolean roundWon;

    /** 이 라운드에서 사망 여부 */
    private boolean died;

    /** 피해·타격 */
    private int damage;
    private int totalShots;
    private int headShots;
    private int bodyShots;
    private int legShots;
    private int kills;
    private int score;

    /** 승리 기여도 점수 (ValorantRoundScoreEngine 산출, 100 기준) */
    private int roundContributionScore;

    /** 부가 지표 */
    private Integer loadoutValue;
    private Integer economyRemaining;
    private Integer economySpent;

    /** 스킬 사용 */
    private int skillXCasts;
    private int skillECasts;
    private int skillQCasts;
    private int skillCCasts;

    /** 행동 플래그 */
    private boolean Afk;
    private boolean Penalized;
    private boolean stayedInSpawn;

    /** 엔트리 지표 (라운드 내 첫 킬/첫 사망) */
    private boolean FirstKill;
    private boolean FirstDeath;

    /** 제거된 적(victim)별 이 플레이어가 가한 데미지. 킬/어시스트 데미지 기여 점수용 */
    private java.util.Map<String, Integer> damageToEliminated;

    /** 내가 킬한 victim별 내 데미지. 킬 데미지 비율(클린업) 보정용 */
    private java.util.Map<String, Integer> myDamagePerKill;
}
