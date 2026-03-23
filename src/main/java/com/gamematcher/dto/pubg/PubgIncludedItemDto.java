package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG match API included 배열의 공통 베이스
 * type에 따라 participant, roster, asset 중 하나로 역직렬화됨
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(name = "participant", value = PubgParticipantIncludedDto.class),
    @JsonSubTypes.Type(name = "roster", value = PubgRosterIncludedDto.class),
    @JsonSubTypes.Type(name = "asset", value = PubgAssetIncludedDto.class)
})
public abstract class PubgIncludedItemDto {

    private String type;
    private String id;

    /** 서브클래스별 attributes (participant/roster/asset) */
    public abstract Object getAttributes();
}
