package com.gamematcher.dto.pubg.map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * PUBG 맵별 지역 매핑 설정.
 * maps/erangel_regions.json 등 구조.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgMapRegionConfig {

    /** 단일 맵 ID (구버전) */
    @JsonProperty("mapId")
    private String mapId;

    /** 복수 맵 ID (Erangel_Main, Baltic_Main 등 동일 레이아웃) */
    @JsonProperty("mapIds")
    private List<String> mapIds;

    private String mapName;
    private Integer gridCols;
    private Integer gridRows;

    /** bounds: { minX, maxX, minY, maxY } */
    private Map<String, Double> bounds;

    /** 그리드 셀 → 지역명 (D4 → Pochinki) */
    private Map<String, String> cells;

    /** POI 목록 (선택) */
    private List<PubgPoiConfig> pois;

    /** 그리드 fallback용 지형명 (선택) */
    private GridConfig grid;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GridConfig {
        private Map<String, String> names;  // D4 → "포친키 인근 평야"
    }

    /**
     * 이 설정이 적용되는 맵 ID 목록.
     */
    public List<String> getApplicableMapIds() {
        if (mapIds != null && !mapIds.isEmpty()) {
            return mapIds;
        }
        if (mapId != null && !mapId.isBlank()) {
            return List.of(mapId);
        }
        return List.of();
    }
}
