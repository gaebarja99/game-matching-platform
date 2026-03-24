package com.gamematcher.dto.ai.evaluation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * PUBG 플레이어 매치 스탯(LLM 프롬프트 입력용).
 *
 * <p>현재 엔진 구조(LoL/Valorant)는 게임별 StatsToPromptFormatter가
 * "사람이 읽기 쉬운 요약 텍스트"로 변환하는 방식을 따르므로,
 * 이 DTO는 그 변환에 필요한 최소 데이터만 담습니다.</p>
 */
@Getter
@ToString
@Builder
public class PubgPlayerMatchStatsDTO {

    private final String game; // "PUBG"
    private final String matchId;

    private final String playerName;
    private final String accountId;
    private final Integer teamId;

    private final Integer winPlace;  // 1이면 승리
    private final boolean won;

    private final Integer kills;
    private final Integer assists;
    private final Double damageDealt;

    private final String mapName;

    private final List<PubgTimelineEventLineDTO> timelineLines;

    public static PubgPlayerMatchStatsDTOBuilder builder() {
        return new PubgPlayerMatchStatsDTOBuilder().timelineLines(new ArrayList<>());
    }
}

