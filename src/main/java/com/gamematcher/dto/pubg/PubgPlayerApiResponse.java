package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PUBG 플레이어 조회 API 응답 DTO
 * GET /shards/{platform}/players?filter[playerNames]={nickname}
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgPlayerApiResponse {

    private List<PubgPlayerDataDto> data;
    private PubgLinksDto links;
    private PubgMetaDto meta;

    /** data 리스트 (플레이어 배열, 보통 1명) */
    public List<PubgPlayerDataDto> getData() {
        return data;
    }
}
