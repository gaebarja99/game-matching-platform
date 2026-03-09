package com.gamematcher.dto.stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gamematcher.constant.GameList;
import org.springframework.stereotype.Component;

/**
 * rawStats JSON ↔ DTO 변환 일원화.
 * ObjectMapper는 JsonSubTypes 설정된 BaseStatsDTO 계층을 인식하도록 구성.
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
     * rawStats JSON 문자열을 GameList에 맞는 DTO로 변환.
     *
     * @param rawStats DB에 저장된 JSON 문자열
     * @param game     게임 종류 (역직렬화 타입 결정에 사용)
     * @return 타입 안전한 BaseStatsDTO (또는 ValorantStatsDTO, LolStatsDTO 등)
     */
    public BaseStatsDTO toDto(String rawStats, GameList game) {
        if (rawStats == null || rawStats.isBlank()) {
            return null;
        }
        try {
            BaseStatsDTO dto = objectMapper.readValue(rawStats, BaseStatsDTO.class);
            if (dto != null && dto.getGame() == null) {
                dto.setGame(game);
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
