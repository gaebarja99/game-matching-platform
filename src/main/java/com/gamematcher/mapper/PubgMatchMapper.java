package com.gamematcher.mapper;

import com.gamematcher.dto.pubg.*;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PUBG 매치 API 응답 DTO → 엔티티 (rosters → participants 그래프).
 */
@Component
public class PubgMatchMapper {

    public PubgMatch toEntity(PubgMatchApiResponse response) {
        if (response == null || response.getData() == null) {
            return null;
        }

        PubgMatchDataDto data = response.getData();
        PubgMatch match = new PubgMatch();
        match.setMatchId(data.getId());

        PubgMatchAttributesDto a = data.getAttributes();
        if (a != null) {
            match.setGameMode(a.getGameMode());
            match.setTitleId(a.getTitleId());
            match.setShardId(a.getShardId());
            match.setMapName(a.getMapName());
            match.setIsCustomMatch(a.getIsCustomMatch());
            match.setSeasonState(a.getSeasonState());
            match.setCreatedAt(a.getCreatedAt());
            match.setDuration(a.getDuration());
            match.setMatchType(a.getMatchType());
        }

        Map<String, PubgIncludedItemDto> byId = new HashMap<>();
        List<PubgIncludedItemDto> included = response.getIncluded();
        if (included != null) {
            for (PubgIncludedItemDto item : included) {
                if (item != null && item.getId() != null) {
                    byId.put(item.getId(), item);
                }
            }
        }

        PubgMatchRelationshipsDto rel = data.getRelationships();
        if (rel == null || rel.getRosters() == null || rel.getRosters().getData() == null) {
            return match;
        }

        for (PubgResourceIdentifierDto rosterRef : rel.getRosters().getData()) {
            if (rosterRef == null || rosterRef.getId() == null) {
                continue;
            }
            PubgIncludedItemDto rosterItem = byId.get(rosterRef.getId());
            if (!(rosterItem instanceof PubgRosterIncludedDto rosterDto)) {
                continue;
            }

            boolean rosterWon = parseRosterWon(rosterDto);
            PubgRosterRelationshipsDto rrel = rosterDto.getRelationships();
            if (rrel == null || rrel.getParticipants() == null || rrel.getParticipants().getData() == null) {
                continue;
            }

            for (PubgResourceIdentifierDto pref : rrel.getParticipants().getData()) {
                if (pref == null || pref.getId() == null) {
                    continue;
                }
                PubgIncludedItemDto pItem = byId.get(pref.getId());
                if (!(pItem instanceof PubgParticipantIncludedDto participantDto)) {
                    continue;
                }

                PubgMatchParticipant part = toParticipant(participantDto, rosterWon);
                part.setMatch(match);
                match.getParticipants().add(part);
            }
        }

        return match;
    }

    private static boolean parseRosterWon(PubgRosterIncludedDto rosterDto) {
        PubgRosterAttributesDto ra = rosterDto.getAttributes();
        if (ra == null || ra.getWon() == null) {
            return false;
        }
        return "true".equalsIgnoreCase(ra.getWon().trim());
    }

    private static PubgMatchParticipant toParticipant(PubgParticipantIncludedDto dto, boolean win) {
        PubgMatchParticipant p = new PubgMatchParticipant();
        p.setParticipantId(dto.getId());

        PubgParticipantAttributesDto attr = dto.getAttributes();
        PubgParticipantStatsDto stats = attr != null ? attr.getStats() : null;

        if (stats != null) {
            p.setPlayerId(stats.getPlayerId() != null ? stats.getPlayerId() : "");
            p.setName(stats.getName());
            p.setDbnos(stats.getDBNOs());
            p.setAssists(stats.getAssists());
            p.setBoosts(stats.getBoosts());
            p.setDamageDealt(stats.getDamageDealt());
            p.setDeathType(stats.getDeathType());
            p.setHeadshotKills(stats.getHeadshotKills());
            p.setHeals(stats.getHeals());
            p.setKillPlace(stats.getKillPlace());
            p.setKillStreaks(stats.getKillStreaks());
            p.setKills(stats.getKills());
            p.setLongestKill(stats.getLongestKill());
            p.setRevives(stats.getRevives());
            p.setRideDistance(stats.getRideDistance());
            p.setRoadKills(stats.getRoadKills());
            p.setSwimDistance(stats.getSwimDistance());
            p.setTeamKills(stats.getTeamKills());
            p.setTimeSurvived(stats.getTimeSurvived());
            p.setVehicleDestroys(stats.getVehicleDestroys());
            p.setWalkDistance(stats.getWalkDistance());
            p.setWeaponsAcquired(stats.getWeaponsAcquired());
            p.setWinPlace(stats.getWinPlace());
        } else {
            p.setPlayerId("");
        }

        p.setWin(win);
        return p;
    }
}
