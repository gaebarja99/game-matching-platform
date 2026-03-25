package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 시즌 data 객체 (API 응답)
 * 형식: { "type": "season", "id": "division.bro.official.pc-2018-40", "attributes": {...} }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgSeasonDataDto {

    private String type;
    private String id;
    private PubgSeasonAttributesDto attributes;
}
