package com.gamematcher.mapper;

import com.gamematcher.dto.pubg.*;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PUBG 매치 DTO → Entity 변환
 */
@Component
public class PubgMatchMapper {

    /**
     * 매치 API 응답 DTO → PubgMatch 엔티티 (participants 포함)
     * PubgMatchApiResponse (data + included) 를 엔티티로 변환
     */
    public PubgMatch toEntity(PubgMatchApiResponse response) {
        if (response == null || response.getData() == null) return null;

        PubgMatch match = new PubgMatch();
        PubgMatchDataDto data = response.getData();
        PubgMatchAttributesDto attrs = data.getAttributes();

        match.setMatchId(data.getId());
        if (attrs != null) {
            match.setGameMode(attrs.getGameMode());
            match.setTitleId(attrs.getTitleId());
            match.setShardId(attrs.getShardId());
            match.setMapName(attrs.getMapName());
            match.setIsCustomMatch(attrs.getIsCustomMatch());
            match.setSeasonState(attrs.getSeasonState());
            match.setCreatedAt(attrs.getCreatedAt());
            match.setDuration(attrs.getDuration());
            match.setMatchType(attrs.getMatchType());
        }

        if (response.getIncluded() != null) {
            for (PubgIncludedItemDto item : response.getIncluded()) {
                if (item instanceof PubgParticipantIncludedDto participantDto) {
                    PubgMatchParticipant participant = toParticipantEntity(match, participantDto);
                    if (participant != null) {
                        match.getParticipants().add(participant);
                    }
                }
            }
        }

        return match;
    }

    /**
     * Participant DTO → PubgMatchParticipant 엔티티
     */
    public PubgMatchParticipant toParticipantEntity(PubgMatch match, PubgParticipantIncludedDto dto) {
        if (match == null || dto == null || dto.getAttributes() == null) return null;

        PubgParticipantAttributesDto attrs = dto.getAttributes();
        PubgParticipantStatsDto stats = attrs.getStats();
        if (stats == null) return null;

        PubgMatchParticipant entity = new PubgMatchParticipant();
        entity.setMatch(match);
        entity.setParticipantId(dto.getId() != null ? dto.getId() : "");
        entity.setPlayerId(stats.getPlayerId() != null ? stats.getPlayerId() : "");
        entity.setName(stats.getName());

        entity.setDbnos(stats.getDBNOs());
        entity.setAssists(stats.getAssists());
        entity.setBoosts(stats.getBoosts());
        entity.setDamageDealt(stats.getDamageDealt());
        entity.setDeathType(stats.getDeathType());
        entity.setHeadshotKills(stats.getHeadshotKills());
        entity.setHeals(stats.getHeals());
        entity.setKillPlace(stats.getKillPlace());
        entity.setKillStreaks(stats.getKillStreaks());
        entity.setKills(stats.getKills());
        entity.setLongestKill(stats.getLongestKill());
        entity.setRevives(stats.getRevives());
        entity.setRideDistance(stats.getRideDistance());
        entity.setRoadKills(stats.getRoadKills());
        entity.setSwimDistance(stats.getSwimDistance());
        entity.setTeamKills(stats.getTeamKills());
        entity.setTimeSurvived(stats.getTimeSurvived());
        entity.setVehicleDestroys(stats.getVehicleDestroys());
        entity.setWalkDistance(stats.getWalkDistance());
        entity.setWeaponsAcquired(stats.getWeaponsAcquired());
        entity.setWinPlace(stats.getWinPlace());
        entity.setWin(Integer.valueOf(1).equals(stats.getWinPlace()));

        return entity;
    }

    /**
     * 플레이어 매치 기록 DTO → PubgMatchParticipant 엔티티
     * PubgPlayerMatchRecordDto (도메인용 요약 DTO) 를 엔티티로 변환
     * match 엔티티는 이미 영속화되어 있어야 함
     */
    public PubgMatchParticipant toParticipantEntity(PubgMatch match, PubgPlayerMatchRecordDto dto) {
        if (match == null || dto == null) return null;

        PubgMatchParticipant entity = new PubgMatchParticipant();
        entity.setMatch(match);
        entity.setParticipantId(dto.getParticipantId() != null ? dto.getParticipantId() : "");
        entity.setPlayerId(dto.getPlayerId() != null ? dto.getPlayerId() : "");
        entity.setName(dto.getPlayerName());

        entity.setDbnos(dto.getDbnos());
        entity.setKills(dto.getKills());
        entity.setAssists(dto.getAssists());
        entity.setDamageDealt(dto.getDamageDealt());
        entity.setHeadshotKills(dto.getHeadshotKills());
        entity.setRevives(dto.getRevives());
        entity.setTimeSurvived(dto.getTimeSurvived());
        entity.setWalkDistance(dto.getWalkDistance());
        entity.setRideDistance(dto.getRideDistance());
        entity.setWinPlace(dto.getWinPlace());
        entity.setKillStreaks(dto.getKillStreaks());
        entity.setWin(Integer.valueOf(1).equals(dto.getWinPlace()));

        return entity;
    }

    /**
     * 여러 PubgPlayerMatchRecordDto → PubgMatchParticipant 리스트
     * 동일 매치에 속한 여러 플레이어 기록을 한 번에 변환
     */
    public List<PubgMatchParticipant> toParticipantEntities(PubgMatch match, List<PubgPlayerMatchRecordDto> records) {
        if (match == null || records == null) return List.of();

        List<PubgMatchParticipant> list = new ArrayList<>();
        for (PubgPlayerMatchRecordDto record : records) {
            PubgMatchParticipant p = toParticipantEntity(match, record);
            if (p != null) list.add(p);
        }
        return list;
    }
}
