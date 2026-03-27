package com.gamematcher.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Records 발로란트: 1차 검색 후 티어만 지연 로드할 때 사용.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValorantSearchMmrRequest {

    private String puuid;
    /** Henrik 샤드 (ap, na, eu, kr, br, latam) — 전적 검색 응답 rawData.valorantRegion */
    private String region;
    private Boolean forceRefresh;

    public void normalize() {
        if (puuid != null) {
            puuid = puuid.trim();
        }
        if (region != null) {
            region = region.trim();
        }
    }
}
