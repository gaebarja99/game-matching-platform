package com.gamematcher.dto.lol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * LoL 타임라인 API 래퍼 응답 DTO
 * - 형식: { "timelines": [...] } 또는 { "data": [...] }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LolMatchTimelineApiResponse {

    @JsonProperty("timelines")
    private List<LolMatchTimelineDetailDto> timelines;

    @JsonProperty("data")
    private List<LolMatchTimelineDetailDto> data;

    /** timelines 또는 data 중 비어있지 않은 목록 반환 */
    public List<LolMatchTimelineDetailDto> getTimelineList() {
        if (timelines != null && !timelines.isEmpty()) {
            return timelines;
        }
        return data;
    }
}
