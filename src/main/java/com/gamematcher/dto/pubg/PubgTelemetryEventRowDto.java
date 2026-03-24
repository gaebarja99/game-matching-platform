package com.gamematcher.dto.pubg;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * PUBG 텔레메트리 이벤트를 DB 저장용(역정규화 통합 1테이블)으로 정규화한 row DTO.
 *
 * <p>원본 event의 가변 구조는 {@link #payloadJson}에 JSON 문자열로 보관하고,
 * 프롬프트 생성에 자주 쓰일 가능성이 있는 값만 상단 컬럼으로 분리합니다.
 */
@Getter
@ToString
@Builder
public class PubgTelemetryEventRowDto {
    private final int eventSequence;
    private final String eventType;
    private final Instant eventTimestamp;

    private final String accountId;
    private final Integer teamId;

    private final Double locationX;
    private final Double locationY;
    private final Double locationZ;
    private final Boolean isInBlueZone;

    private final String itemId;
    private final String itemCategory;

    /** event 전체를 담은 JSON 문자열 (원본 keys 포함). */
    private final String payloadJson;
}

