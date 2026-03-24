package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 랭크 API data 객체
 * GET /shards/{platform}/players/{accountId}/seasons/{seasonId}/ranked
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRankedPlayerStatsDataDto {

    private String type;
    private String id;
    private PubgRankedPlayerStatsAttributesDto attributes;
    private PubgRankedPlayerStatsRelationshipsDto relationships;
}
