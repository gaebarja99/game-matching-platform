package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Valorant 전적 API 응답 JSON → DTO 매핑 서비스
 * HenrikDev API 응답 형식 지원
 */
@Service
public class ValorantMatchJsonService {

    private final ObjectMapper objectMapper;

    public ValorantMatchJsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * API 전체 응답 JSON 파싱
     * 형식: { "status": 200, "data": [ ValorantMatchDetailDto, ... ] }
     *
     * @param json API 응답 JSON 문자열
     * @return ValorantMatchApiResponse (status, data 목록 포함)
     */
    public ValorantMatchApiResponse parseApiResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, ValorantMatchApiResponse.class);
        } catch (IOException e) {
            throw new ValorantMatchJsonParseException("Valorant API 응답 JSON 파싱 실패", e);
        }
    }

    /**
     * API 응답 JSON을 쪼개어 data 배열의 매치 목록으로 반환
     * 형식: { "status": 200, "data": [ {...}, {...}, ... ] }
     *
     * @param json API 응답 JSON 문자열
     * @return 매치 DTO 목록 (data가 null 또는 비어있으면 빈 리스트)
     */
    public List<ValorantMatchDetailDto> parseMatchesFromApiResponse(String json) {
        ValorantMatchApiResponse response = parseApiResponse(json);
        List<ValorantMatchDetailDto> data = response.getData();
        return data != null ? data : Collections.emptyList();
    }

    /**
     * 단일 매치 상세 JSON 파싱
     * 형식: { "is_available": true, "metadata": {...}, "players": {...}, "rounds": [...], ... }
     *
     * @param json 매치 상세 JSON 문자열
     * @return ValorantMatchDetailDto
     */
    public ValorantMatchDetailDto parseMatchDetail(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, ValorantMatchDetailDto.class);
        } catch (IOException e) {
            throw new ValorantMatchJsonParseException("Valorant 매치 상세 JSON 파싱 실패", e);
        }
    }

    /**
     * API 응답 또는 단일 매치 JSON에서 첫 번째 매치 추출
     * - API 응답 형식({ "status", "data": [...] })이면 data[0] 반환
     * - 단일 매치 형식({ "metadata", "players", ... })이면 그대로 반환
     *
     * @param json JSON 문자열 (API 응답 또는 단일 매치)
     * @return ValorantMatchDetailDto
     */
    public ValorantMatchDetailDto parseFirstMatch(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            var tree = objectMapper.readTree(json);
            if (tree.has("data") && tree.get("data").isArray() && tree.get("data").size() > 0) {
                return objectMapper.treeToValue(tree.get("data").get(0), ValorantMatchDetailDto.class);
            }
            if (tree.has("metadata") && tree.has("players")) {
                return objectMapper.treeToValue(tree, ValorantMatchDetailDto.class);
            }
            throw new ValorantMatchJsonParseException("유효한 Valorant 전적 JSON 형식이 아닙니다 (data 배열 또는 metadata/players 필요)");
        } catch (IOException e) {
            throw new ValorantMatchJsonParseException("Valorant 전적 JSON 파싱 실패", e);
        }
    }

    /**
     * Valorant 전적 JSON 파싱 예외
     */
    public static class ValorantMatchJsonParseException extends RuntimeException {
        public ValorantMatchJsonParseException(String message) {
            super(message);
        }

        public ValorantMatchJsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
