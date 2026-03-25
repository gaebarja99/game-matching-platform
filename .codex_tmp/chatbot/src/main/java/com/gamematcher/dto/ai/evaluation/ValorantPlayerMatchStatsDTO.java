package com.gamematcher.dto.ai.evaluation;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 특정 매치의 특정 플레이어 스탯 (매치 전체 + 라운드별).
 * AI 분석 시 한 유저의 매치·라운드 데이터를 함께 전달.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ValorantPlayerMatchStatsDTO {

    /** 매치 식별자 */
    private String matchId;

    /** 플레이어 식별자 */
    private String playerPuuid;
    private String playerDisplayName;
    private String playerTeam;
    /** 에이전트 */
    private String agent;

    /** 매치 전체 스탯 (KDA, KAST, ADR, 멀티킬 등) */
    private ValorantMatchStatsDTO matchStats;

    /** 라운드별 스탯 (roundIndex 순) */
    @Builder.Default
    private List<ValorantRoundStatsDTO> roundStats = new ArrayList<>();
}
