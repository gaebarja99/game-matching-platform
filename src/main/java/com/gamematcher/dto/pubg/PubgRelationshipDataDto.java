package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PUBG JSON:API relationship data wrapper
 * 형식: { "data": [{"type":"roster","id":"..."}, ...] }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRelationshipDataDto {

    private List<PubgResourceIdentifierDto> data;
}
