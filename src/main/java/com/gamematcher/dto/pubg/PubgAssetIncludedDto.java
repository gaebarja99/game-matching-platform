package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG included type: asset
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgAssetIncludedDto extends PubgIncludedItemDto {

    @Getter(AccessLevel.NONE)
    private PubgAssetAttributesDto attributes;

    @Override
    public PubgAssetAttributesDto getAttributes() {
        return attributes;
    }
}
