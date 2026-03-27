package com.gamematcher.mapper;

import com.gamematcher.dto.lol.LolAccountResponseDto;
import com.gamematcher.entity.account.LolAccount;
import org.springframework.stereotype.Component;

/**
 * LoL 계정 DTO → Entity 변환
 */
@Component
public class LolAccountMapper {

    /**
     * LolAccountResponseDto → LolAccount 엔티티
     */
    public LolAccount toEntity(LolAccountResponseDto dto) {
        if (dto == null || dto.getPuuid() == null || dto.getPuuid().isBlank()) {
            return null;
        }

        LolAccount entity = new LolAccount();
        entity.setPuuid(dto.getPuuid());
        entity.setGameName(dto.getGameName() != null ? dto.getGameName() : "");
        entity.setTagLine(dto.getTagLine() != null ? dto.getTagLine() : "");
        return entity;
    }
}
