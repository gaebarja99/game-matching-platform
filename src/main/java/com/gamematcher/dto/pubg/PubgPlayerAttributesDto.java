package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG 플레이어 attributes
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgPlayerAttributesDto {

    private String patchVersion;
    private String banType;
    private String clanId;
    private String name;
    private Object stats;
    private String titleId;
    private String shardId;
}
