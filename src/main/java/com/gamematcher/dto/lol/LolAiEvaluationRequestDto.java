package com.gamematcher.dto.lol;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LoL 플레이어 AI 평가 API 요청 DTO.
 *
 * <p>대상 지정 우선순위: {@code playerPuuid} &gt; {@code playerDisplayName} (Riot ID 형식 {@code 게임명#태그}) &gt; {@code playerIndex}.</p>
 */
@Data
@NoArgsConstructor
public class LolAiEvaluationRequestDto {

    /** 매치 JSON (필수, 단일 또는 matches/data 래퍼 형태) */
    @NotNull(message = "matchJson은 필수입니다.")
    private String matchJson;

    /** 타임라인 JSON (선택, 없으면 타임라인 이벤트 없이 분석) */
    private String timelineJson;

    /** Riot 계정 PUUID (매치 participants와 일치) */
    private String playerPuuid;

    /**
     * 소환사 표시명. 매치 DTO의 {@code riotIdGameName#riotIdTagline} 형식과 동일하게 넣으면 된다 (대소문자 무시).
     */
    private String playerDisplayName;

    /**
     * 평가할 플레이어의 participants 순서 인덱스 (0-based).
     * {@code playerPuuid} / {@code playerDisplayName} 이 없을 때만 사용한다.
     */
    @Min(0)
    @Max(9)
    private Integer playerIndex;
}
