package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PUBG 매치 상세 API 응답 DTO
 * 형식: { "data": {...}, "included": [...], "links": {...}, "meta": {} }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgMatchApiResponse {

    private PubgMatchDataDto data;
    private List<PubgIncludedItemDto> included;
    private PubgLinksDto links;
    private PubgMetaDto meta;
}
