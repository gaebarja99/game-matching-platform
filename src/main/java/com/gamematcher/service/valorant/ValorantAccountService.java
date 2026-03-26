package com.gamematcher.service.valorant;

import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import com.gamematcher.entity.account.ValorantAccount;
import com.gamematcher.mapper.ValorantAccountMapper;
import com.gamematcher.repository.account.ValorantAccountRepository;
import com.gamematcher.service.MatchApiCachePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Valorant 계정 DTO → DB 저장 서비스
 */
@Service
public class ValorantAccountService {

    private final ValorantAccountRepository repository;
    private final ValorantAccountMapper mapper;

    public ValorantAccountService(ValorantAccountRepository repository,
                                  ValorantAccountMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Valorant 계정 DTO를 DB에 저장 (중복 시 업데이트)
     *
     * @param dto AccountData DTO
     * @return 저장/수정된 엔티티
     */
    /**
     * 닉·태그로 캐시된 계정이 있고, API 캐시가 신선하면 Henrik 호출 없이 응답을 재구성한다.
     */
    @Transactional(readOnly = true)
    public Optional<ValorantPuuidApiResponse> findFreshCachedAccountResponse(String name, String tag) {
        if (name == null || name.isBlank() || tag == null || tag.isBlank()) {
            return Optional.empty();
        }
        return repository.findByNameIgnoreCaseAndTagIgnoreCase(name.trim(), tag.trim())
                .filter(row -> !MatchApiCachePolicy.isStale(row.getApiCachedAt()))
                .map(mapper::toPuuidApiResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ValorantPuuidApiResponse> findAnyCachedAccountResponse(String name, String tag) {
        if (name == null || name.isBlank() || tag == null || tag.isBlank()) {
            return Optional.empty();
        }
        return repository.findByNameIgnoreCaseAndTagIgnoreCase(name.trim(), tag.trim())
                .map(mapper::toPuuidApiResponse);
    }

    @Transactional
    public ValorantAccount saveAccount(ValorantPuuidApiResponse.AccountData dto) {
        ValorantAccount entity = mapper.toEntity(dto);
        if (entity == null) {
            return null;
        }
        LocalDateTime cachedAt = LocalDateTime.now();

        return repository.findByPuuid(entity.getPuuid())
                .map(existing -> {
                    existing.setRegion(entity.getRegion());
                    existing.setAccountLevel(entity.getAccountLevel());
                    existing.setName(entity.getName());
                    existing.setTag(entity.getTag());
                    existing.setCardId(entity.getCardId());
                    existing.setCardSmall(entity.getCardSmall());
                    existing.setCardLarge(entity.getCardLarge());
                    existing.setCardWide(entity.getCardWide());
                    existing.setLastUpdate(entity.getLastUpdate());
                    existing.setLastUpdateRaw(entity.getLastUpdateRaw());
                    existing.setApiCachedAt(cachedAt);
                    return repository.save(existing);
                })
                .orElseGet(() -> {
                    entity.setApiCachedAt(cachedAt);
                    return repository.save(entity);
                });
    }

    /**
     * ValorantPuuidApiResponse 전체 응답을 DB에 저장
     *
     * @param response API 응답
     * @return 저장/수정된 엔티티, 데이터 없으면 null
     */
    @Transactional
    public ValorantAccount saveAccount(ValorantPuuidApiResponse response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        return saveAccount(response.getData());
    }
}
