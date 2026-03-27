package com.gamematcher.dto.lol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * LoL 매치 ID 목록 API 응답 DTO (Riot Match-v5 by-puuid/ids).
 * Riot API는 매치 ID 배열을 직접 반환: ["KR_123", "KR_456", ...]
 */
@Getter
@Setter
@NoArgsConstructor
public class LolMatchIdListResponseDto {

    private List<String> matchIds;

    @JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING)
    public LolMatchIdListResponseDto(List<String> matchIds) {
        this.matchIds = matchIds != null ? matchIds : Collections.emptyList();
    }
}
