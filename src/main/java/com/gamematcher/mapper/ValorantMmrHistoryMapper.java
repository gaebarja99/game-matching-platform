package com.gamematcher.mapper;

import com.gamematcher.dto.valorant.ValorantMmrHistoryApiResponse;
import com.gamematcher.entity.match.valorant.ValorantMmrHistoryRecord;
import org.springframework.stereotype.Component;

/**
 * Valorant MMR History DTO → Entity 변환
 */
@Component
public class ValorantMmrHistoryMapper {

    /**
     * MmrHistoryItem + puuid → ValorantMmrHistoryRecord
     */
    public ValorantMmrHistoryRecord toEntity(String puuid, ValorantMmrHistoryApiResponse.MmrHistoryItem dto) {
        if (dto == null || puuid == null || puuid.isBlank() || dto.getMatchId() == null) {
            return null;
        }

        ValorantMmrHistoryRecord entity = new ValorantMmrHistoryRecord();
        entity.setPuuid(puuid);
        entity.setMatchId(dto.getMatchId());
        entity.setCurrentTier(dto.getCurrenttier());
        entity.setCurrentTierPatched(dto.getCurrentTierPatched());
        entity.setSeasonId(dto.getSeasonId());
        entity.setRankingInTier(dto.getRankingInTier());
        entity.setMmrChangeToLastGame(dto.getMmrChangeToLastGame());
        entity.setElo(dto.getElo());
        entity.setDate(dto.getDate());
        entity.setDateRaw(dto.getDateRaw());

        if (dto.getMap() != null) {
            entity.setMapId(dto.getMap().getId());
            entity.setMapName(dto.getMap().getName());
        }
        if (dto.getImages() != null) {
            entity.setImageSmall(dto.getImages().getSmall());
            entity.setImageLarge(dto.getImages().getLarge());
        }

        return entity;
    }
}
