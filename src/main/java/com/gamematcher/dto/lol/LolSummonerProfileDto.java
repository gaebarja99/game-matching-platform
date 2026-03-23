package com.gamematcher.dto.lol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LoL 소환사 프로필 API 응답 DTO (Riot Summoner-v4 by-puuid).
 * src/test/resources/samples/lol/lol_profile_sample.json 구조
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LolSummonerProfileDto {

    @JsonProperty("puuid")
    private String puuid;

    @JsonProperty("profileIconId")
    private Integer profileIconId;

    @JsonProperty("revisionDate")
    private Long revisionDate;

    @JsonProperty("summonerLevel")
    private Integer summonerLevel;
}
