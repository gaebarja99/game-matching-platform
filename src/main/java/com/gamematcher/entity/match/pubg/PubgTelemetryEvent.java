package com.gamematcher.entity.match.pubg;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * PUBG 텔레메트리 이벤트 (역정규화 통합 1테이블).
 *
 * <p>프롬프트 생성이 주 목적이므로, 타입별 상세 필드는 최소 컬럼만 분리하고
 * 나머지는 {@code payloadJson}에 JSON으로 보관합니다.</p>
 */
@Entity
@Table(
        name = "pubg_telemetry_event",
        indexes = {
                @Index(columnList = "match_id, event_type, event_timestamp"),
                @Index(columnList = "match_id, account_id, event_timestamp"),
                @Index(columnList = "match_id, item_id, event_timestamp")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"match_id", "event_sequence"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PubgTelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private PubgMatch match;

    @Column(name = "event_sequence", nullable = false)
    private int eventSequence;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_timestamp")
    private Instant eventTimestamp;

    @Column(name = "account_id", length = 80)
    private String accountId;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "location_x")
    private Double locationX;

    @Column(name = "location_y")
    private Double locationY;

    @Column(name = "location_z")
    private Double locationZ;

    @Column(name = "is_in_bluezone")
    private Boolean isInBlueZone;

    @Column(name = "item_id", length = 100)
    private String itemId;

    @Column(name = "item_category", length = 50)
    private String itemCategory;

    @Column(name = "payload_json", columnDefinition = "json")
    private String payloadJson;
}

