package com.gamematcher.service.lol;

import com.gamematcher.dto.lol.LolSummonerProfileDto;
import com.gamematcher.entity.account.LolSummonerProfile;
import com.gamematcher.mapper.LolSummonerProfileMapper;
import com.gamematcher.repository.account.LolSummonerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LoL 소환사 프로필 DTO → DB 저장 서비스 (LolSummonerProfileDto)
 */
@Service
public class LolSummonerProfileService {

    private final LolSummonerProfileRepository repository;
    private final LolSummonerProfileMapper mapper;

    public LolSummonerProfileService(LolSummonerProfileRepository repository,
                                    LolSummonerProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * LoL 소환사 프로필 DTO를 DB에 저장 (중복 시 업데이트)
     *
     * @param dto Riot Summoner-v4 API 응답 DTO
     * @return 저장/수정된 엔티티
     */
    @Transactional
    public LolSummonerProfile saveProfile(LolSummonerProfileDto dto) {
        LolSummonerProfile entity = mapper.toEntity(dto);
        if (entity == null) {
            return null;
        }

        return repository.findByPuuid(entity.getPuuid())
                .map(existing -> {
                    existing.setProfileIconId(entity.getProfileIconId());
                    existing.setRevisionDate(entity.getRevisionDate());
                    existing.setSummonerLevel(entity.getSummonerLevel());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(entity));
    }
}
