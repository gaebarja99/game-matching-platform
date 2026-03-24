package com.gamematcher.mapper;

import com.gamematcher.dto.valorant.ValorantLifetimeDataItem;
import com.gamematcher.entity.match.valorant.ValorantLifetimeRecord;
import org.springframework.stereotype.Component;

/**
 * Valorant Lifetime DTO → Entity 변환
 */
@Component
public class ValorantLifetimeMapper {

    /**
     * ValorantLifetimeDataItem DTO → ValorantLifetimeRecord 엔티티
     */
    public ValorantLifetimeRecord toEntity(ValorantLifetimeDataItem dto) {
        if (dto == null || dto.getStats() == null) {
            return null;
        }

        ValorantLifetimeRecord entity = new ValorantLifetimeRecord();

        // meta
        if (dto.getMeta() != null) {
            var meta = dto.getMeta();
            String metaId = meta.getId();
            entity.setRiotMatchId(metaId);
            entity.setMatchId(metaId);
            entity.setVersion(meta.getVersion());
            entity.setMode(meta.getMode());
            entity.setStartedAt(meta.getStartedAt());
            entity.setRegion(meta.getRegion());
            entity.setCluster(meta.getCluster());
            if (meta.getMap() != null) {
                entity.setMapId(meta.getMap().getId());
                entity.setMapName(meta.getMap().getName());
            }
            if (meta.getSeason() != null) {
                entity.setSeasonId(meta.getSeason().getId());
                entity.setSeasonShort(meta.getSeason().getShortName());
            }
        }

        // stats
        var stats = dto.getStats();
        entity.setPuuid(stats.getPuuid());
        entity.setName(stats.getName());
        entity.setTag(stats.getTag());
        entity.setTeam(stats.getTeam());
        entity.setLevel(stats.getLevel());
        entity.setTier(stats.getTier());
        entity.setScore(stats.getScore());
        entity.setKills(stats.getKills());
        entity.setDeaths(stats.getDeaths());
        entity.setAssists(stats.getAssists());
        if (stats.getCharacter() != null) {
            entity.setCharacterId(stats.getCharacter().getId());
            entity.setCharacterName(stats.getCharacter().getName());
        }
        if (stats.getShots() != null) {
            entity.setShotsHead(stats.getShots().getHead());
            entity.setShotsBody(stats.getShots().getBody());
            entity.setShotsLeg(stats.getShots().getLeg());
        }
        if (stats.getDamage() != null) {
            entity.setDamageMade(stats.getDamage().getMade());
            entity.setDamageReceived(stats.getDamage().getReceived());
        }

        // teams
        if (dto.getTeams() != null) {
            entity.setRedRounds(dto.getTeams().getRed());
            entity.setBlueRounds(dto.getTeams().getBlue());
        }

        return entity;
    }
}
