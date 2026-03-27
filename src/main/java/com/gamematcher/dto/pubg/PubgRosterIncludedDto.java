package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG included type: roster
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRosterIncludedDto extends PubgIncludedItemDto {

    @Getter(AccessLevel.NONE)
    private PubgRosterAttributesDto attributes;
    private PubgRosterRelationshipsDto relationships;

    @Override
    public PubgRosterAttributesDto getAttributes() {
        return attributes;
    }
}
