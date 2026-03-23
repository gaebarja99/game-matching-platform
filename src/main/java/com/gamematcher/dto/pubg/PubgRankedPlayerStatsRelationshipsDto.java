package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 랭크 API data.relationships
 * player, season은 단일 리소스 참조
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRankedPlayerStatsRelationshipsDto {

    /** 플레이어 참조 (data: { type, id }) */
    private PubgRelationshipSingleDataDto player;

    /** 시즌 참조 (data: { type, id }) */
    private PubgRelationshipSingleDataDto season;
}
