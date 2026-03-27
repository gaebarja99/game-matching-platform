package com.gamematcher.dto.ai.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 라인 상대(Lane Opponent)와의 비교 지표.
 * Match API challenges + Timeline participantFrames 기반.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LolLaneComparisonDto {

    /** 10분 시점 골드 차이 (양수=우리 유리) */
    private Integer goldDiffAt10;

    /** 15분 시점 골드 차이 */
    private Integer goldDiffAt15;

    /** 10분 시점 CS 차이 */
    private Integer csDiffAt10;

    /** 15분 시점 CS 차이 */
    private Integer csDiffAt15;

    /** 10분 시점 경험치 차이 */
    private Integer xpDiffAt10;

    /** 15분 시점 경험치 차이 */
    private Integer xpDiffAt15;

    /** challenges.maxCsAdvantageOnLaneOpponent (라인战中 최대 CS 유리) */
    private Double maxCsAdvantageOnLaneOpponent;

    /** challenges.laningPhaseGoldExpAdvantage (라인전 골드 우위) */
    private Double laningPhaseGoldExpAdvantage;

    /** 5분 단위 스냅샷 (라인 상대 + 팀 전체 비교) */
    private List<LolTimelineSnapshotDto> snapshots;
}
