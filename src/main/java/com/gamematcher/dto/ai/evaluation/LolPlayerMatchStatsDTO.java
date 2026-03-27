package com.gamematcher.dto.ai.evaluation;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * LoL 특정 매치의 특정 플레이어 스탯.
 * AI 분석 시 한 유저의 매치 데이터를 전달한다.
 * 발로란트 {@link ValorantPlayerMatchStatsDTO}와 유사한 구조.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LolPlayerMatchStatsDTO {

    /** 매치 식별자 */
    private String matchId;

    /** 플레이어 식별자 */
    private String playerPuuid;
    private String playerDisplayName;

    /** 챔피언 및 포지션 */
    private String champion;
    private String teamPosition;

    /** 매치 전체 스탯 (KDA, CS, 딜량, 시야 등) */
    private LolStatsDTO matchStats;

    /** 타임라인 기반 이벤트 요약 (상세 분석 시 사용, 선택) */
    @Builder.Default
    private List<LolTimelineEventSummaryDTO> timelineEvents = new ArrayList<>();

    /** 라인전 상대 비교 (10분/15분 골드·CS·XP 차이) */
    private LolLaneComparisonDto laneComparison;
}
