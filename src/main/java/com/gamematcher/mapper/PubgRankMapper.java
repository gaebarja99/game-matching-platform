package com.gamematcher.mapper;

import com.gamematcher.dto.pubg.PubgRankedGameModeStatsDto;
import com.gamematcher.dto.pubg.PubgRankedPlayerStatsApiResponse;
import com.gamematcher.dto.pubg.PubgRankedPlayerStatsAttributesDto;
import com.gamematcher.dto.pubg.PubgTierDto;
import com.gamematcher.entity.match.pubg.PubgPlayerRank;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PUBG 랭크 API DTO → Entity 변환
 */
@Component
public class PubgRankMapper {

    /**
     * 랭크 API 응답 → tier 문자열 (squad-fpp 우선, 없으면 squad)
     * UI 표시용 (예: "Survivor 1")
     */
    public String toTierDisplayString(PubgRankedPlayerStatsApiResponse response) {
        if (response == null || response.getData() == null) return "UNRANKED";

        PubgRankedGameModeStatsDto modeStats = getPreferredModeStats(response);
        if (modeStats == null || modeStats.getCurrentTier() == null) return "UNRANKED";

        PubgTierDto tier = modeStats.getCurrentTier();
        String tierStr = tier.getTier() != null ? tier.getTier() : "UNRANKED";
        String subTier = tier.getSubTier() != null && !tier.getSubTier().isEmpty()
                ? " " + tier.getSubTier() : "";
        return tierStr + subTier;
    }

    /**
     * squad-fpp 우선, 없으면 squad
     */
    private PubgRankedGameModeStatsDto getPreferredModeStats(PubgRankedPlayerStatsApiResponse response) {
        PubgRankedPlayerStatsAttributesDto attrs = response.getData().getAttributes();
        if (attrs == null || attrs.getRankedGameModeStats() == null) return null;

        Map<String, PubgRankedGameModeStatsDto> modeStats = attrs.getRankedGameModeStats();
        PubgRankedGameModeStatsDto mode = modeStats.get("squad-fpp");
        if (mode == null) mode = modeStats.get("squad");
        return mode;
    }

    /**
     * 랭크 API 응답 → PubgPlayerRank 엔티티 목록 (게임 모드별 1 row)
     *
     * @param response API 응답
     * @param playerId 플레이어 account ID (relationships.player.data.id)
     * @param seasonId 시즌 ID (relationships.season.data.id)
     * @param platform 플랫폼 (steam, psn, xbox, kakao)
     */
    public List<PubgPlayerRank> toEntities(PubgRankedPlayerStatsApiResponse response,
                                         String playerId, String seasonId, String platform) {
        if (response == null || response.getData() == null) return List.of();

        PubgRankedPlayerStatsAttributesDto attrs = response.getData().getAttributes();
        if (attrs == null || attrs.getRankedGameModeStats() == null) return List.of();

        // relationships에서 player/season ID 사용 (파라미터로 전달된 값 우선)
        if (playerId == null && response.getData().getRelationships() != null
                && response.getData().getRelationships().getPlayer() != null
                && response.getData().getRelationships().getPlayer().getData() != null) {
            playerId = response.getData().getRelationships().getPlayer().getData().getId();
        }
        if (seasonId == null && response.getData().getRelationships() != null
                && response.getData().getRelationships().getSeason() != null
                && response.getData().getRelationships().getSeason().getData() != null) {
            seasonId = response.getData().getRelationships().getSeason().getData().getId();
        }
        if (playerId == null || seasonId == null || platform == null) return List.of();

        List<PubgPlayerRank> entities = new ArrayList<>();
        for (Map.Entry<String, PubgRankedGameModeStatsDto> entry : attrs.getRankedGameModeStats().entrySet()) {
            PubgPlayerRank entity = toEntity(playerId, seasonId, platform, entry.getKey(), entry.getValue());
            if (entity != null) entities.add(entity);
        }
        return entities;
    }

    private PubgPlayerRank toEntity(String playerId, String seasonId, String platform,
                                    String gameMode, PubgRankedGameModeStatsDto dto) {
        if (dto == null) return null;

        PubgPlayerRank entity = new PubgPlayerRank();
        entity.setPlayerId(playerId);
        entity.setSeasonId(seasonId);
        entity.setPlatform(platform);
        entity.setGameMode(gameMode);

        if (dto.getCurrentTier() != null) {
            entity.setCurrentTier(dto.getCurrentTier().getTier());
            entity.setSubTier(dto.getCurrentTier().getSubTier());
        }
        entity.setCurrentRankPoint(dto.getCurrentRankPoint());

        if (dto.getBestTier() != null) {
            entity.setBestTier(dto.getBestTier().getTier());
            entity.setBestSubTier(dto.getBestTier().getSubTier());
        }
        entity.setBestRankPoint(dto.getBestRankPoint());

        entity.setRoundsPlayed(dto.getRoundsPlayed());
        entity.setAvgRank(dto.getAvgRank());
        entity.setAvgSurvivalTime(dto.getAvgSurvivalTime());
        entity.setTop10Ratio(dto.getTop10Ratio());
        entity.setWinRatio(dto.getWinRatio());
        entity.setWins(dto.getWins());
        entity.setKills(dto.getKills());
        entity.setDeaths(dto.getDeaths());
        entity.setAssists(dto.getAssists());
        entity.setAvgKill(dto.getAvgKill());
        entity.setRoundMostKills(dto.getRoundMostKills());
        entity.setLongestKill(dto.getLongestKill());
        entity.setHeadshotKills(dto.getHeadshotKills());
        entity.setHeadshotKillRatio(dto.getHeadshotKillRatio());
        entity.setDamageDealt(dto.getDamageDealt());
        entity.setDbnos(dto.getDBNOs());
        entity.setReviveRatio(dto.getReviveRatio());
        entity.setRevives(dto.getRevives());
        entity.setHeals(dto.getHeals());
        entity.setBoosts(dto.getBoosts());
        entity.setWeaponsAcquired(dto.getWeaponsAcquired());
        entity.setTeamKills(dto.getTeamKills());
        entity.setPlayTime(dto.getPlayTime());
        entity.setKillStreak(dto.getKillStreak());

        return entity;
    }
}
