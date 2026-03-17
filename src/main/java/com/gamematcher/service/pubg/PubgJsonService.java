package com.gamematcher.service.pubg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.pubg.PubgMatchApiResponse;
import com.gamematcher.dto.pubg.PubgPlayerApiResponse;
import com.gamematcher.dto.pubg.PubgTelemetryEventDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * PUBG API 응답 JSON → DTO 매핑 서비스
 * 각 API별로 별도 메서드 제공
 *
 * @see <a href="https://developer.pubg.com/">PUBG API 문서</a>
 */
@Service
public class PubgJsonService {

    private static final TypeReference<List<PubgTelemetryEventDto>> TELEMETRY_LIST_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public PubgJsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 매치 상세 API 응답 JSON 파싱
     * GET /shards/{platform}/matches/{matchId}
     * 형식: { "data": {...}, "included": [...], "links": {...}, "meta": {} }
     *
     * @param json 매치 API 응답 JSON 문자열
     * @return PubgMatchApiResponse
     */
    public PubgMatchApiResponse parseMatchResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, PubgMatchApiResponse.class);
        } catch (IOException e) {
            throw new PubgJsonParseException("PUBG 매치 API 응답 JSON 파싱 실패", e);
        }
    }

    /**
     * 플레이어 조회 API 응답 JSON 파싱
     * GET /shards/{platform}/players?filter[playerNames]={nickname}
     * 형식: { "data": [...], "links": {...}, "meta": {} }
     *
     * @param json 플레이어 API 응답 JSON 문자열
     * @return PubgPlayerApiResponse
     */
    public PubgPlayerApiResponse parsePlayerResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, PubgPlayerApiResponse.class);
        } catch (IOException e) {
            throw new PubgJsonParseException("PUBG 플레이어 API 응답 JSON 파싱 실패", e);
        }
    }

    /**
     * 텔레메트리 API 응답 JSON 파싱
     * GET {asset.attributes.URL} (매치 included의 asset에서 URL 획득)
     * 형식: [ { "_T": "LogMatchDefinition", "_D": "...", ... }, ... ]
     *
     * @param json 텔레메트리 API 응답 JSON 문자열 (객체 배열)
     * @return 텔레메트리 이벤트 DTO 목록 (비어있으면 빈 리스트)
     */
    public List<PubgTelemetryEventDto> parseTelemetryResponse(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<PubgTelemetryEventDto> list = objectMapper.readValue(json, TELEMETRY_LIST_TYPE);
            return list != null ? list : Collections.emptyList();
        } catch (IOException e) {
            throw new PubgJsonParseException("PUBG 텔레메트리 API 응답 JSON 파싱 실패", e);
        }
    }

    /**
     * PUBG JSON 파싱 예외
     */
    public static class PubgJsonParseException extends RuntimeException {
        public PubgJsonParseException(String message) {
            super(message);
        }

        public PubgJsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
