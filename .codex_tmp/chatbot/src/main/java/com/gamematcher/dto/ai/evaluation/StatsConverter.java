package com.gamematcher.dto.ai.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

/**
 * rawStats JSON ↔ DTO 변환 일원화.
 * ObjectMapper는 JsonSubTypes 설정된 BaseStatsDTO 계층을 인식하도록 구성.
 * DB 중심: gameCode(String)로 전용 DTO/GenericStatsDTO 자동 분기.
 */
@Component
public class StatsConverter {

    private final ObjectMapper objectMapper;

    public StatsConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * rawStats JSON 문자열을 DTO로 변환.
     *
     * @param rawStats  DB에 저장된 JSON 문자열
     * @param gameCode  게임 코드 (JSON에 game이 없을 때 fallback, Game.code 사용)
     * @return 타입 안전한 BaseStatsDTO (ValorantStatsDTO, LolStatsDTO, GenericStatsDTO 등)
     */
    public BaseStatsDTO toDto(String rawStats, String gameCode) {
        if (rawStats == null || rawStats.isBlank()) {
            return null;
        }
        try {
            BaseStatsDTO dto = objectMapper.readValue(rawStats, BaseStatsDTO.class);
            if (dto != null && (dto.getGame() == null || dto.getGame().isBlank())) {
                dto.setGame(gameCode);
            }
            return dto;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("rawStats JSON 파싱 실패: " + rawStats, e);
        }
    }

    /**
     * DTO를 JSON 문자열로 직렬화.
     *
     * @param dto BaseStatsDTO (또는 하위 타입)
     * @return JSON 문자열
     */
    public String toJson(BaseStatsDTO dto) {
        if (dto == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("DTO JSON 직렬화 실패", e);
        }
    }
}
