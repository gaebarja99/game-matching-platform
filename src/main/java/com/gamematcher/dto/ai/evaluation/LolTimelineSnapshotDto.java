package com.gamematcher.dto.ai.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 5분 단위 타임라인 스냅샷.
 * 라인 상대 비교 + 팀 전체 비교 지표.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LolTimelineSnapshotDto {

    /** 기준 시간(분) */
    private int minute;

    /** 라인 상대 대비 골드 차이 (양수=나 유리) */
    private Integer goldDiffLane;

    /** 라인 상대 대비 CS 차이 */
    private Integer csDiffLane;

    /** 라인 상대 대비 경험치 차이 */
    private Integer xpDiffLane;

    /** 팀 전체 골드 차이 (우리팀-상대팀, 양수=우리 유리) */
    private Integer goldDiffTeam;

    /** 팀 전체 CS 차이 */
    private Integer csDiffTeam;

    /** 팀 전체 경험치 차이 */
    private Integer xpDiffTeam;
}
