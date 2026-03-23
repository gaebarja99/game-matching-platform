package com.gamematcher.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.entity.match.valorant.ValorantMmrRecord;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Valorant MMR DTO → Entity 변환
 */
@Component
public class ValorantMmrMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ValorantMmrApiResponse.MmrData → ValorantMmrRecord 엔티티
     */
    public ValorantMmrRecord toEntity(ValorantMmrApiResponse.MmrData dto) {
        if (dto == null || dto.getPuuid() == null || dto.getPuuid().isBlank()) {
            return null;
        }

        ValorantMmrRecord entity = new ValorantMmrRecord();
        entity.setPuuid(dto.getPuuid());
        entity.setName(dto.getName());
        entity.setTag(dto.getTag());

        if (dto.getCurrentData() != null) {
            var cd = dto.getCurrentData();
            entity.setCurrentTier(cd.getCurrenttier());
            entity.setCurrentTierPatched(cd.getCurrentTierPatched());
            entity.setRankingInTier(cd.getRankingInTier());
            entity.setMmrChangeToLastGame(cd.getMmrChangeToLastGame());
            entity.setElo(cd.getElo());
            entity.setGamesNeededForRating(cd.getGamesNeededForRating());
            if (cd.getImages() != null) {
                entity.setImageSmall(cd.getImages().getSmall());
                entity.setImageLarge(cd.getImages().getLarge());
            }
        }

        if (dto.getHighestRank() != null) {
            var hr = dto.getHighestRank();
            entity.setHighestTier(hr.getTier());
            entity.setHighestTierPatched(hr.getPatchedTier());
            entity.setHighestSeason(hr.getSeason());
        }

        if (dto.getBySeason() != null && !dto.getBySeason().isEmpty()) {
            try {
                entity.setBySeasonJson(objectMapper.writeValueAsString(dto.getBySeason()));
            } catch (JsonProcessingException e) {
                entity.setBySeasonJson("{}");
            }
        }

        return entity;
    }

    /**
     * ValorantMmrApiResponse 전체 응답에서 MmrData 추출 후 엔티티 변환
     */
    public ValorantMmrRecord toEntity(ValorantMmrApiResponse response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        return toEntity(response.getData());
    }
}
