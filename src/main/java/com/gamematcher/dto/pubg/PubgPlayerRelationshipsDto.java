package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PUBG 플레이어 relationships
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgPlayerRelationshipsDto {

    private PubgRelationshipDataDto assets;
    private PubgRelationshipDataDto matches;

    /** matches.data 에서 매치 ID 목록 */
    public List<PubgResourceIdentifierDto> getMatchIds() {
        return matches != null ? matches.getData() : null;
    }
}
