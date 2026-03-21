package com.gamematcher.dto.ai.evaluation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 프롬프트용 PUBG 타임라인 한 줄.
 */
@Getter
@ToString
@Builder
public class PubgTimelineEventLineDTO {
    /**
     * 라벨: [운영]/[위험]/[교전]/[결과]/[회복] 등
     */
    private final String label;

    /** match 시작 기준 경과초 (프롬프트에 넣기 좋은 값) */
    private final long elapsedSeconds;

    /** 사람이 읽는 한 줄 메시지 (시간 포함해서 만들거나, 시간만 포맷해서 합성 가능) */
    private final String message;
}

