package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 랭크 API 응답 DTO
 * GET /shards/{platform}/players/{accountId}/seasons/{seasonId}/ranked
 * 형식: { "data": {...}, "links": {...}, "meta": {} }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRankedPlayerStatsApiResponse {

    private PubgRankedPlayerStatsDataDto data;
    private PubgLinksDto links;
    private PubgMetaDto meta;
}
