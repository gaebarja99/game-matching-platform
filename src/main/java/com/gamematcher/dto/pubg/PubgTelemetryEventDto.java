package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PUBG 텔레메트리 API 이벤트 DTO
 * asset URL에서 가져오는 JSON 배열의 각 요소
 * _T: 이벤트 타입 (LogMatchDefinition, LogPlayerLogin, LogPlayerCreate 등)
 * _D: 타임스탬프
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgTelemetryEventDto {

    @JsonProperty("_T")
    private String type;

    @JsonProperty("_D")
    private String timestamp;

    /** _T, _D 외 나머지 필드 (character, accountId, common 등) */
    private final Map<String, Object> additional = new LinkedHashMap<>();

    @JsonAnySetter
    public void setAdditional(String key, Object value) {
        if (!"_T".equals(key) && !"_D".equals(key)) {
            additional.put(key, value);
        }
    }
}
