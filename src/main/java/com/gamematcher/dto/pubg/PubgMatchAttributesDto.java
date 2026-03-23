package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 매치 attributes
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgMatchAttributesDto {

    private String gameMode;
    private String titleId;
    private String shardId;
    private Object tags;
    private String mapName;
    private Boolean isCustomMatch;
    private String seasonState;

    @JsonProperty("createdAt")
    private String createdAt;

    private Integer duration;
    private String matchType;
    private Object stats;
}
