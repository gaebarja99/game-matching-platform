package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 플레이어 data 객체
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgPlayerDataDto {

    private String type;
    private String id;
    private PubgPlayerAttributesDto attributes;
    private PubgPlayerRelationshipsDto relationships;
    private PubgResourceLinksDto links;
}
