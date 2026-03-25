package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 매치 data 객체 (JSON:API 형식)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgMatchDataDto {

    private String type;
    private String id;
    private PubgMatchAttributesDto attributes;
    private PubgMatchRelationshipsDto relationships;
    private PubgResourceLinksDto links;
}
