package com.gamematcher.mapper;

import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import com.gamematcher.entity.account.ValorantAccount;
import org.springframework.stereotype.Component;

/**
 * Valorant 계정 DTO → Entity 변환
 */
@Component
public class ValorantAccountMapper {

    /**
     * ValorantPuuidApiResponse.AccountData → ValorantAccount 엔티티
     */
    public ValorantAccount toEntity(ValorantPuuidApiResponse.AccountData dto) {
        if (dto == null || dto.getPuuid() == null || dto.getPuuid().isBlank()) {
            return null;
        }

        ValorantAccount entity = new ValorantAccount();
        entity.setPuuid(dto.getPuuid());
        entity.setRegion(dto.getRegion());
        entity.setAccountLevel(dto.getAccountLevel());
        entity.setName(dto.getName());
        entity.setTag(dto.getTag());
        entity.setLastUpdate(dto.getLastUpdate());
        entity.setLastUpdateRaw(dto.getLastUpdateRaw());

        if (dto.getCard() != null) {
            entity.setCardId(dto.getCard().getId());
            entity.setCardSmall(dto.getCard().getSmall());
            entity.setCardLarge(dto.getCard().getLarge());
            entity.setCardWide(dto.getCard().getWide());
        }

        return entity;
    }

    /**
     * ValorantPuuidApiResponse 전체 응답에서 AccountData 추출 후 엔티티 변환
     */
    public ValorantAccount toEntity(ValorantPuuidApiResponse response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        return toEntity(response.getData());
    }
}
