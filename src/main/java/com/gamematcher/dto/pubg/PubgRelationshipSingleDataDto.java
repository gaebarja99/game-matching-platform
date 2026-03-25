package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG JSON:API 단일 리소스 relationship
 * 형식: { "data": { "type": "player", "id": "account.xxx" } }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRelationshipSingleDataDto {

    private PubgResourceIdentifierDto data;
}
