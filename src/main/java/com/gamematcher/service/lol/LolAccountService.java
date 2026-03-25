package com.gamematcher.service.lol;

import com.gamematcher.dto.lol.LolAccountResponseDto;
import com.gamematcher.entity.account.LolAccount;
import com.gamematcher.mapper.LolAccountMapper;
import com.gamematcher.repository.account.LolAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LoL 계정 DTO → DB 저장 서비스 (LolAccountResponseDto)
 */
@Service
public class LolAccountService {

    private final LolAccountRepository repository;
    private final LolAccountMapper mapper;

    public LolAccountService(LolAccountRepository repository,
                             LolAccountMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * LoL 계정 DTO를 DB에 저장 (중복 시 업데이트)
     *
     * @param dto Riot Account-v1 API 응답 DTO
     * @return 저장/수정된 엔티티
     */
    @Transactional
    public LolAccount saveAccount(LolAccountResponseDto dto) {
        LolAccount entity = mapper.toEntity(dto);
        if (entity == null) {
            return null;
        }

        return repository.findByPuuid(entity.getPuuid())
                .map(existing -> {
                    existing.setGameName(entity.getGameName());
                    existing.setTagLine(entity.getTagLine());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(entity));
    }
}
