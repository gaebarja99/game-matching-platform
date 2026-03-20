package com.gamematcher.dto.pubg.map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PUBG 맵 POI(Point of Interest) 설정.
 * 중심점 + 반경으로 지역 판별.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgPoiConfig {

    private String id;
    private String nameKo;
    private String type;  // urban, mountain, coastal 등
    private List<Double> center;  // [x, y]
    private Double radius;
    private Double nearRadius;  // 이 거리 이내면 "포친키 남서쪽 150m" 형태
}
