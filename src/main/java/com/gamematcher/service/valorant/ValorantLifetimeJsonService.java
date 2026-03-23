package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.valorant.ValorantLifetimeApiResponse;
import com.gamematcher.dto.valorant.ValorantLifetimeDataItem;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Valorant Lifetime API 응답 JSON → DTO 매핑 서비스
 * 형식: { "status": 200, "results": {...}, "data": [ ValorantLifetimeDataItem, ... ] }
 */
@Service
public class ValorantLifetimeJsonService {

    private final ObjectMapper objectMapper;

    public ValorantLifetimeJsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * API 전체 응답 JSON 파싱
     * 형식: { "status": 200, "results": {...}, "data": [ ValorantLifetimeDataItem, ... ] }
     *
     * @param json API 응답 JSON 문자열
     * @return ValorantLifetimeApiResponse (status, results, data 목록 포함)
     */
    public ValorantLifetimeApiResponse parseApiResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, ValorantLifetimeApiResponse.class);
        } catch (IOException e) {
            throw new ValorantLifetimeJsonParseException("Valorant Lifetime API 응답 JSON 파싱 실패", e);
        }
    }

    /**
     * API 응답 JSON을 쪼개어 data 배열의 Lifetime 항목 목록으로 반환
     * 형식: { "status": 200, "results": {...}, "data": [ {...}, {...}, ... ] }
     *
     * @param json API 응답 JSON 문자열
     * @return Lifetime DTO 목록 (data가 null 또는 비어있으면 빈 리스트)
     */
    public List<ValorantLifetimeDataItem> parseDataFromApiResponse(String json) {
        ValorantLifetimeApiResponse response = parseApiResponse(json);
        List<ValorantLifetimeDataItem> data = response.getData();
        return data != null ? data : Collections.emptyList();
    }

    /**
     * Valorant Lifetime JSON 파싱 예외
     */
    public static class ValorantLifetimeJsonParseException extends RuntimeException {
        public ValorantLifetimeJsonParseException(String message) {
            super(message);
        }

        public ValorantLifetimeJsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
