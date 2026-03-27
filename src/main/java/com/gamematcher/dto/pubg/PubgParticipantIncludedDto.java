package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG included type: participant
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgParticipantIncludedDto extends PubgIncludedItemDto {

    @Getter(AccessLevel.NONE)
    private PubgParticipantAttributesDto attributes;

    @Override
    public PubgParticipantAttributesDto getAttributes() {
        return attributes;
    }
}

