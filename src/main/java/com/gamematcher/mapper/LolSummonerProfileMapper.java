package com.gamematcher.mapper;

import com.gamematcher.dto.lol.LolSummonerProfileDto;
import com.gamematcher.entity.account.LolSummonerProfile;
import org.springframework.stereotype.Component;

/**
 * LoL 소환사 프로필 DTO → Entity 변환
 */
@Component
public class LolSummonerProfileMapper {

    /**
     * LolSummonerProfileDto → LolSummonerProfile 엔티티
     */
    public LolSummonerProfile toEntity(LolSummonerProfileDto dto) {
        if (dto == null || dto.getPuuid() == null || dto.getPuuid().isBlank()) {
            return null;
        }

        LolSummonerProfile entity = new LolSummonerProfile();
        entity.setPuuid(dto.getPuuid());
        entity.setProfileIconId(dto.getProfileIconId());
        entity.setRevisionDate(dto.getRevisionDate());
        entity.setSummonerLevel(dto.getSummonerLevel());
        return entity;
    }
}
