package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * PUBG 랭크 API data.attributes
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRankedPlayerStatsAttributesDto {

    /** 게임 모드별 스탯 (squad, squad-fpp, duo, duo-fpp, solo, solo-fpp) */
    private Map<String, PubgRankedGameModeStatsDto> rankedGameModeStats;
}
