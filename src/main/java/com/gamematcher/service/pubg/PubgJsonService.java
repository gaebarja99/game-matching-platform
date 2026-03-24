package com.gamematcher.service.pubg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.pubg.PubgMatchApiResponse;
import org.springframework.stereotype.Service;

/**
 * PUBG 매치 API JSON → DTO 파싱 (테스트·도구용).
 */
@Service
public class PubgJsonService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PubgMatchApiResponse parseMatchResponse(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readValue(json, PubgMatchApiResponse.class);
    }
}
