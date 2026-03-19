package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PUBG 시즌 목록 API 응답 DTO
 * 형식: { "data": [...], "links": {...}, "meta": {} }
 * 엔드포인트: https://api.pubg.com/shards/{platform}/seasons
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgSeasonsApiResponse {

    private List<PubgSeasonDataDto> data;
    private PubgLinksDto links;
    private PubgMetaDto meta;
}
