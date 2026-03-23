package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 시즌 attributes (API 응답)
 * 형식: { "isCurrentSeason": true, "isOffseason": false }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgSeasonAttributesDto {

    @JsonProperty("isCurrentSeason")
    private Boolean isCurrentSeason;

    @JsonProperty("isOffseason")
    private Boolean isOffseason;
}
