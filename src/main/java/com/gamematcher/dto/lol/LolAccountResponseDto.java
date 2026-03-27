package com.gamematcher.dto.lol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LoL 계정 정보 API 응답 DTO (Riot Account-v1 by-riot-id).
 * src/test/resources/samples/lol/lol_puuid_sample.json 구조
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LolAccountResponseDto {

    @JsonProperty("puuid")
    private String puuid;

    @JsonProperty("gameName")
    private String gameName;

    @JsonProperty("tagLine")
    private String tagLine;
}
