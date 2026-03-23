package com.gamematcher.dto.valorant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Valorant 매치 API 전체 응답 래퍼
 * { "status": 200, "data": [ValorantMatchDetailDto, ...] }
 */
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatchApiResponse {

    private int status;
    private List<ValorantMatchDetailDto> data;
}
