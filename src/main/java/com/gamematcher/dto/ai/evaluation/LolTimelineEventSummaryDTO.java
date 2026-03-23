package com.gamematcher.dto.ai.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LoL 타임라인 이벤트 요약 (프롬프트용 압축 데이터).
 * CHAMPION_KILL: playerRole(Kill/Death/Assist), BUILDING_KILL 등 핵심 이벤트 추출.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LolTimelineEventSummaryDTO {

    /** 이벤트 타입 (CHAMPION_KILL, BUILDING_KILL, DRAGON_SOUL_GIVEN 등) */
    private String type;

    /** 게임 내 타임스탬프 (ms) */
    private Long timestamp;

    /** 게임 내 시간(분) */
    private Integer minute;

    /** 참여자 participantId (킬러, 킬당한 대상 등) */
    private Integer participantId;

    /** 추가 정보 (오브젝트 타입, 킬/데스 관련 등) */
    private String detail;

    /** CHAMPION_KILL에서 이 플레이어 역할: Kill, Death, Assist */
    private String playerRole;

    /** 이벤트 발생 위치 요약 (예: 상대 탑 근처, 바론 구역) - 동선 분석용 */
    private String positionBrief;

    /** 그룹핑 키 (TURRET_PLATE_DESTROYED 축약용: "PLATE_3_7") */
    private String groupKey;
}
