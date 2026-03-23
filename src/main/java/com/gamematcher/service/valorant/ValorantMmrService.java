package com.gamematcher.service.valorant;

import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.entity.match.valorant.ValorantMmrRecord;
import com.gamematcher.mapper.ValorantMmrMapper;
import com.gamematcher.repository.match.ValorantMmrRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valorant MMR DTO → DB 저장 서비스
 */
@Service
public class ValorantMmrService {

    private final ValorantMmrRecordRepository repository;
    private final ValorantMmrMapper mapper;

    public ValorantMmrService(ValorantMmrRecordRepository repository,
                             ValorantMmrMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Valorant MMR DTO를 DB에 저장 (중복 시 업데이트)
     *
     * @param dto MmrData DTO
     * @return 저장/수정된 엔티티
     */
    @Transactional
    public ValorantMmrRecord saveMmr(ValorantMmrApiResponse.MmrData dto) {
        ValorantMmrRecord entity = mapper.toEntity(dto);
        if (entity == null) {
            return null;
        }

        return repository.findByPuuid(entity.getPuuid())
                .map(existing -> {
                    existing.setName(entity.getName());
                    existing.setTag(entity.getTag());
                    existing.setCurrentTier(entity.getCurrentTier());
                    existing.setCurrentTierPatched(entity.getCurrentTierPatched());
                    existing.setRankingInTier(entity.getRankingInTier());
                    existing.setMmrChangeToLastGame(entity.getMmrChangeToLastGame());
                    existing.setElo(entity.getElo());
                    existing.setGamesNeededForRating(entity.getGamesNeededForRating());
                    existing.setImageSmall(entity.getImageSmall());
                    existing.setImageLarge(entity.getImageLarge());
                    existing.setHighestTier(entity.getHighestTier());
                    existing.setHighestTierPatched(entity.getHighestTierPatched());
                    existing.setHighestSeason(entity.getHighestSeason());
                    existing.setBySeasonJson(entity.getBySeasonJson());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(entity));
    }

    /**
     * ValorantMmrApiResponse 전체 응답을 DB에 저장
     *
     * @param response API 응답
     * @return 저장/수정된 엔티티, 데이터 없으면 null
     */
    @Transactional
    public ValorantMmrRecord saveMmr(ValorantMmrApiResponse response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        return saveMmr(response.getData());
    }
}
