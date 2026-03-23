package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG included asset attributes (telemetry URL 등)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgAssetAttributesDto {

    private String name;
    private String description;
    private String createdAt;

    @JsonProperty("URL")
    private String url;  // JSON: "URL"
}
