package com.gamematcher.dto.pubg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 특정 플레이어의 배틀그라운드 매치 기록 (도메인용).
 * API 응답을 파싱한 후 플레이어별로 정리한 데이터.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PubgPlayerMatchRecordDto {

    /** 매치 ID */
    private String matchId;
    /** 매치 생성 시각 */
    private Instant matchCreatedAt;
    /** 게임 모드 (tdm, squad, solo 등) */
    private String gameMode;
    /** 맵 이름 */
    private String mapName;
    /** 매치 지속 시간(초) */
    private Integer duration;

    /** 플레이어 account ID */
    private String playerId;
    /** 플레이어 닉네임 */
    private String playerName;
    /** 참가자 엔티티 ID (included 내 participant id) */
    private String participantId;

    /** 처치 */
    private Integer kills;
    /** 헤드샷 처치 */
    private Integer headshotKills;
    /** 어시스트 */
    private Integer assists;
    /** 입힌 데미지 */
    private Double damageDealt;
    /** DBNO (쓰러뜨림) */
    private Integer dbnos;
    /** 부활 */
    private Integer revives;
    /** 생존 시간(초) */
    private Integer timeSurvived;
    /** 도보 거리 */
    private Double walkDistance;
    /** 차량 이동 거리 */
    private Double rideDistance;
    /** 최종 순위 (1=1등) */
    private Integer winPlace;
    /** 연속 처치 */
    private Integer killStreaks;
}
