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

    private int roundIndex;
    private String playerPuuid;
    private String playerDisplayName;
    private String playerTeam;

    /** 피해·타격 */
    private int damage;
    private int headshots;
    private int bodyshots;
    private int legshots;
    private int kills;
    private int score;

    /** 부가 지표 */
    private Integer loadoutValue;
    private Integer economyRemaining;
    private Integer economySpent;

    /** 스킬 사용 */
    private int abilityXCasts;
    private int abilityECasts;
    private int abilityQCasts;
    private int abilityCCasts;

    /** 행동 플래그 */
    private boolean wasAfk;
    private boolean wasPenalized;
    private boolean stayedInSpawn;

    /** 엔트리 지표 (라운드 내 첫 킬/첫 사망) */
    private boolean gotFirstBlood;
    private boolean wasFirstDeath;

    /** 라운드 결과 */
    private boolean roundWon;
}
