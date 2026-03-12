package com.gamematcher.service.valorant;

import com.gamematcher.dto.valorant.ValorantMmrHistoryApiResponse;
import com.gamematcher.entity.match.valorant.ValorantMmrHistoryRecord;
import com.gamematcher.mapper.ValorantMmrHistoryMapper;
import com.gamematcher.repository.match.ValorantMmrHistoryRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Valorant MMR History DTO → DB 저장 서비스
 * puuid는 API 호출 시 URL에 포함되므로 저장 시 파라미터로 전달
 */
@Service
public class ValorantMmrHistoryService {

    private final ValorantMmrHistoryRecordRepository repository;
    private final ValorantMmrHistoryMapper mapper;

    public ValorantMmrHistoryService(ValorantMmrHistoryRecordRepository repository,
                                     ValorantMmrHistoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * 단일 MMR History 항목 저장 (중복 시 스킵)
     */
    @Transactional
    public ValorantMmrHistoryRecord saveRecord(String puuid, ValorantMmrHistoryApiResponse.MmrHistoryItem dto) {
        ValorantMmrHistoryRecord entity = mapper.toEntity(puuid, dto);
        if (entity == null) {
            return null;
        }
        if (repository.existsByPuuidAndMatchId(entity.getPuuid(), entity.getMatchId())) {
            return null;
        }
        return repository.save(entity);
    }

    /**
     * MMR History 전체 응답 저장 (puuid는 API 컨텍스트에서 전달)
     *
     * @param puuid   API URL의 puuid (응답에 없음)
     * @param response API 응답
     * @param limit   저장할 최대 개수 (0이면 전체)
     * @return 저장된 개수
     */
    @Transactional
    public int saveRecords(String puuid, ValorantMmrHistoryApiResponse response, int limit) {
        if (puuid == null || puuid.isBlank() || response == null || response.getData() == null) {
            return 0;
        }
        List<ValorantMmrHistoryApiResponse.MmrHistoryItem> items = response.getData();
        if (items.isEmpty()) {
            return 0;
        }
        int max = limit > 0 ? Math.min(limit, items.size()) : items.size();
        int saved = 0;
        for (int i = 0; i < max; i++) {
            if (saveRecord(puuid, items.get(i)) != null) {
                saved++;
            }
        }
        return saved;
    }
}
