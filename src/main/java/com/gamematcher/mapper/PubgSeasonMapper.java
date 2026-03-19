package com.gamematcher.mapper;

import com.gamematcher.dto.pubg.PubgSeasonAttributesDto;
import com.gamematcher.dto.pubg.PubgSeasonDataDto;
import com.gamematcher.dto.pubg.PubgSeasonsApiResponse;
import com.gamematcher.entity.match.pubg.PubgSeason;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PUBG 시즌 DTO → Entity 변환
 */
@Component
public class PubgSeasonMapper {

    /**
     * PubgSeasonDataDto → PubgSeason 엔티티
     *
     * @param dto      시즌 DTO (API data 배열 내 항목)
     * @param platform 플랫폼 (steam, psn, xbox, kakao 등) - API 응답에는 없어서 외부 전달
     */
    public PubgSeason toEntity(PubgSeasonDataDto dto, String platform) {
        if (dto == null || dto.getId() == null || dto.getId().isBlank()) {
            return null;
        }
        if (platform == null || platform.isBlank()) {
            platform = "steam";
        }

        PubgSeason entity = new PubgSeason();
        entity.setPlatform(platform);
        entity.setSeasonId(dto.getId());

        PubgSeasonAttributesDto attrs = dto.getAttributes();
        if (attrs != null) {
            entity.setIsCurrentSeason(attrs.getIsCurrentSeason());
            entity.setIsOffseason(attrs.getIsOffseason());
        }

        return entity;
    }

    /**
     * PubgSeasonsApiResponse 전체 응답 → PubgSeason 엔티티 목록
     *
     * @param response API 전체 응답
     * @param platform 플랫폼 (steam, psn, xbox, kakao 등)
     */
    public List<PubgSeason> toEntities(PubgSeasonsApiResponse response, String platform) {
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return Collections.emptyList();
        }
        return response.getData().stream()
                .map(dto -> toEntity(dto, platform))
                .filter(e -> e != null)
                .collect(Collectors.toList());
    }
}
