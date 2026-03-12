package com.gamematcher.service.valorant;

import com.gamematcher.dto.valorant.ValorantLifetimeDataItem;
import com.gamematcher.entity.match.valorant.ValorantLifetimeRecord;
import com.gamematcher.mapper.ValorantLifetimeMapper;
import com.gamematcher.repository.match.ValorantLifetimeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Valorant Lifetime DTO → DB 저장 서비스
 */
@Service
public class ValorantLifetimeService {

    private final ValorantLifetimeRecordRepository repository;
    private final ValorantLifetimeMapper mapper;

    public ValorantLifetimeService(ValorantLifetimeRecordRepository repository,
                                   ValorantLifetimeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * 단일 Lifetime DTO를 DB에 저장 (중복 시 스킵)
     *
     * @param dto Lifetime DTO
     * @return 저장된 엔티티, 이미 존재하거나 유효하지 않으면 null
     */
    @Transactional
    public ValorantLifetimeRecord saveRecord(ValorantLifetimeDataItem dto) {
        ValorantLifetimeRecord entity = mapper.toEntity(dto);
        if (entity == null || entity.getMatchId() == null || entity.getPuuid() == null) {
            return null;
        }
        if (repository.existsByMatchIdAndPuuid(entity.getMatchId(), entity.getPuuid())) {
            return null;
        }
        return repository.save(entity);
    }

    /**
     * Lifetime DTO 목록을 DB에 저장 (중복 스킵)
     *
     * @param dtos Lifetime DTO 목록
     * @param limit 저장할 최대 개수 (0이면 제한 없음)
     * @return 저장된 개수
     */
    @Transactional
    public int saveRecords(List<ValorantLifetimeDataItem> dtos, int limit) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        int saved = 0;
        int max = limit > 0 ? Math.min(limit, dtos.size()) : dtos.size();

        for (int i = 0; i < max; i++) {
            ValorantLifetimeRecord savedRecord = saveRecord(dtos.get(i));
            if (savedRecord != null) {
                saved++;
            }
        }
        return saved;
    }
}
