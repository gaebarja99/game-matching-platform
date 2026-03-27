package com.gamematcher.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.lol.*;
import com.gamematcher.entity.match.lol.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LoL 매치 DTO ↔ Entity 변환
 */
@Component
public class LolMatchMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 매치 상세 DTO → LolMatch 엔티티 (participants, teams 포함)
     */
    public LolMatch toMatchEntity(LolMatchDetailDto dto) {
        if (dto == null) return null;

        LolMatch match = new LolMatch();

        if (dto.getMetadata() != null) {
            match.setMatchId(dto.getMetadata().getMatchId());
            match.setDataVersion(dto.getMetadata().getDataVersion());
        }

        if (dto.getInfo() != null) {
            var info = dto.getInfo();
            match.setGameId(info.getGameId());
            match.setGameCreation(info.getGameCreation());
            match.setGameDuration(info.getGameDuration());
            match.setGameStartTimestamp(info.getGameStartTimestamp());
            match.setGameEndTimestamp(info.getGameEndTimestamp());
            match.setGameMode(info.getGameMode());
            match.setGameType(info.getGameType());
            match.setGameName(info.getGameName());
            match.setGameVersion(info.getGameVersion());
            match.setMapId(info.getMapId());
            match.setQueueId(info.getQueueId());
            match.setPlatformId(info.getPlatformId());
            match.setEndOfGameResult(info.getEndOfGameResult());
            match.setTournamentCode(info.getTournamentCode());

            if (info.getParticipants() != null) {
                for (LolParticipantDto p : info.getParticipants()) {
                    match.getParticipants().add(toParticipantEntity(match, p));
                }
            }

            if (info.getTeams() != null) {
                for (LolMatchDetailDto.Info.Team t : info.getTeams()) {
                    match.getTeams().add(toTeamEntity(match, t));
                }
            }
        }

        return match;
    }

    private LolMatchParticipant toParticipantEntity(LolMatch match, LolParticipantDto p) {
        LolMatchParticipant entity = new LolMatchParticipant();
        entity.setMatch(match);
        entity.setParticipantId(p.getParticipantId() != null ? p.getParticipantId() : 0);
        entity.setPuuid(p.getPuuid() != null ? p.getPuuid() : "");
        entity.setSummonerId(p.getSummonerId());
        entity.setRiotIdGameName(p.getRiotIdGameName());
        entity.setRiotIdTagline(p.getRiotIdTagline());
        entity.setChampionId(p.getChampionId());
        entity.setChampionName(p.getChampionName());
        entity.setTeamId(p.getTeamId());
        entity.setIndividualPosition(p.getIndividualPosition());
        entity.setTeamPosition(p.getTeamPosition());
        entity.setKills(p.getKills());
        entity.setDeaths(p.getDeaths());
        entity.setAssists(p.getAssists());
        entity.setWin(Boolean.TRUE.equals(p.getWin()));
        entity.setGoldEarned(p.getGoldEarned());
        entity.setGoldSpent(p.getGoldSpent());
        entity.setTotalDamageDealtToChampions(p.getTotalDamageDealtToChampions());
        entity.setTotalDamageTaken(p.getTotalDamageTaken());
        entity.setVisionScore(p.getVisionScore());
        entity.setTotalMinionsKilled(p.getTotalMinionsKilled());
        entity.setNeutralMinionsKilled(p.getNeutralMinionsKilled());
        entity.setChampLevel(p.getChampLevel());
        entity.setItem0(p.getItem0());
        entity.setItem1(p.getItem1());
        entity.setItem2(p.getItem2());
        entity.setItem3(p.getItem3());
        entity.setItem4(p.getItem4());
        entity.setItem5(p.getItem5());
        entity.setItem6(p.getItem6());
        entity.setSummoner1Id(p.getSummoner1Id());
        entity.setSummoner2Id(p.getSummoner2Id());
        return entity;
    }

    private LolMatchTeam toTeamEntity(LolMatch match, LolMatchDetailDto.Info.Team t) {
        LolMatchTeam entity = new LolMatchTeam();
        entity.setMatch(match);
        entity.setTeamId(t.getTeamId() != null ? t.getTeamId() : 0);
        entity.setWin(Boolean.TRUE.equals(t.getWin()));
        try {
            entity.setBans(t.getBans() != null ? objectMapper.writeValueAsString(t.getBans()) : null);
            entity.setObjectives(t.getObjectives() != null ? objectMapper.writeValueAsString(t.getObjectives()) : null);
        } catch (Exception e) {
            entity.setBans(null);
            entity.setObjectives(null);
        }
        return entity;
    }

    /**
     * 타임라인 DTO → LolMatchTimeline 엔티티
     * @param match 이미 저장된 LolMatch
     */
    public LolMatchTimeline toTimelineEntity(LolMatch match, LolMatchTimelineDetailDto dto) {
        if (match == null || dto == null) return null;

        LolMatchTimeline entity = new LolMatchTimeline();
        entity.setMatch(match);

        if (dto.getInfo() != null) {
            entity.setFrameInterval(dto.getInfo().getFrameInterval());
            entity.setEndOfGameResult(dto.getInfo().getEndOfGameResult());
            entity.setTimelineInfo(toTimelineInfoJson(dto.getInfo()));
        }

        return entity;
    }

    /** LolMatchTimelineDetailDto.Info → JSON 문자열 저장용 */
    private String toTimelineInfoJson(LolMatchTimelineDetailDto.Info info) {
        try {
            return info != null ? objectMapper.writeValueAsString(info) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * DB에 저장된 타임라인(JSON) → DTO (AI 프롬프트·스탯 매퍼용).
     */
    public LolMatchTimelineDetailDto toTimelineDetailDto(LolMatchTimeline entity) {
        if (entity == null || entity.getTimelineInfo() == null || entity.getTimelineInfo().isBlank()) {
            return null;
        }
        try {
            LolMatchTimelineDetailDto dto = new LolMatchTimelineDetailDto();
            LolMatchTimelineDetailDto.Info info = objectMapper.readValue(
                    entity.getTimelineInfo(), LolMatchTimelineDetailDto.Info.class);
            dto.setInfo(info);
            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * LolMatch 엔티티 → DTO (DB → API 응답용)
     */
    public LolMatchDetailDto toMatchDto(LolMatch match) {
        if (match == null) return null;

        LolMatchDetailDto dto = new LolMatchDetailDto();
        dto.setMetadata(toMetadataDto(match));
        dto.setInfo(toInfoDto(match));
        return dto;
    }

    private LolMatchDetailDto.Metadata toMetadataDto(LolMatch match) {
        LolMatchDetailDto.Metadata meta = new LolMatchDetailDto.Metadata();
        meta.setMatchId(match.getMatchId());
        meta.setDataVersion(match.getDataVersion());
        meta.setParticipants(match.getParticipants().stream()
                .map(LolMatchParticipant::getPuuid)
                .toList());
        return meta;
    }

    private LolMatchDetailDto.Info toInfoDto(LolMatch match) {
        LolMatchDetailDto.Info info = new LolMatchDetailDto.Info();
        info.setGameId(match.getGameId());
        info.setGameCreation(match.getGameCreation());
        info.setGameDuration(match.getGameDuration());
        info.setGameStartTimestamp(match.getGameStartTimestamp());
        info.setGameEndTimestamp(match.getGameEndTimestamp());
        info.setGameMode(match.getGameMode());
        info.setGameType(match.getGameType());
        info.setGameName(match.getGameName());
        info.setGameVersion(match.getGameVersion());
        info.setMapId(match.getMapId());
        info.setQueueId(match.getQueueId());
        info.setPlatformId(match.getPlatformId());
        info.setEndOfGameResult(match.getEndOfGameResult());
        info.setTournamentCode(match.getTournamentCode());

        List<LolParticipantDto> participants = new ArrayList<>();
        for (LolMatchParticipant p : match.getParticipants()) {
            participants.add(toParticipantDto(p));
        }
        info.setParticipants(participants);

        List<LolMatchDetailDto.Info.Team> teams = new ArrayList<>();
        for (LolMatchTeam t : match.getTeams()) {
            teams.add(toTeamDto(t));
        }
        info.setTeams(teams);

        return info;
    }

    private LolParticipantDto toParticipantDto(LolMatchParticipant p) {
        LolParticipantDto dto = new LolParticipantDto();
        dto.setParticipantId(p.getParticipantId());
        dto.setPuuid(p.getPuuid());
        dto.setSummonerId(p.getSummonerId());
        dto.setRiotIdGameName(p.getRiotIdGameName());
        dto.setRiotIdTagline(p.getRiotIdTagline());
        dto.setChampionId(p.getChampionId());
        dto.setChampionName(p.getChampionName());
        dto.setTeamId(p.getTeamId());
        dto.setIndividualPosition(p.getIndividualPosition());
        dto.setTeamPosition(p.getTeamPosition());
        dto.setKills(p.getKills());
        dto.setDeaths(p.getDeaths());
        dto.setAssists(p.getAssists());
        dto.setWin(p.isWin());
        dto.setGoldEarned(p.getGoldEarned());
        dto.setGoldSpent(p.getGoldSpent());
        dto.setTotalDamageDealtToChampions(p.getTotalDamageDealtToChampions());
        dto.setTotalDamageTaken(p.getTotalDamageTaken());
        dto.setVisionScore(p.getVisionScore());
        dto.setTotalMinionsKilled(p.getTotalMinionsKilled());
        dto.setNeutralMinionsKilled(p.getNeutralMinionsKilled());
        dto.setChampLevel(p.getChampLevel());
        dto.setItem0(p.getItem0());
        dto.setItem1(p.getItem1());
        dto.setItem2(p.getItem2());
        dto.setItem3(p.getItem3());
        dto.setItem4(p.getItem4());
        dto.setItem5(p.getItem5());
        dto.setItem6(p.getItem6());
        dto.setSummoner1Id(p.getSummoner1Id());
        dto.setSummoner2Id(p.getSummoner2Id());
        return dto;
    }

    private LolMatchDetailDto.Info.Team toTeamDto(LolMatchTeam t) {
        LolMatchDetailDto.Info.Team team = new LolMatchDetailDto.Info.Team();
        team.setTeamId(t.getTeamId());
        team.setWin(t.isWin());
        try {
            if (t.getBans() != null && !t.getBans().isBlank()) {
                team.setBans(objectMapper.readValue(t.getBans(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, LolMatchDetailDto.Info.Team.Ban.class)));
            }
            if (t.getObjectives() != null && !t.getObjectives().isBlank()) {
                team.setObjectives(objectMapper.readValue(t.getObjectives(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, LolMatchDetailDto.Info.Team.Objective.class)));
            }
        } catch (Exception e) {
            // JSON 파싱 실패 시 무시
        }
        return team;
    }
}
