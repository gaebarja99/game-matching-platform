package com.gamematcher.service.valorant;

import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.mapper.ValorantMatchMapper;
import com.gamematcher.repository.match.ValorantMatchDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Valorant 매치 DTO → DB 저장 서비스
 */
@Service
public class ValorantMatchService {

    private final ValorantMatchDetailRepository valorantMatchDetailRepository;
    private final ValorantMatchMapper valorantMatchMapper;

    public ValorantMatchService(ValorantMatchDetailRepository valorantMatchDetailRepository,
                               ValorantMatchMapper valorantMatchMapper) {
        this.valorantMatchDetailRepository = valorantMatchDetailRepository;
        this.valorantMatchMapper = valorantMatchMapper;
    }

    /**
     * 단일 매치 DTO를 DB에 저장 (중복 시 스킵)
     *
     * @param dto 매치 상세 DTO
     * @return 저장된 매치, 이미 존재하면 null
     */
    @Transactional
    public ValorantMatch saveMatch(ValorantMatchDetailDto dto) {
        if (dto == null || dto.getMetadata() == null || dto.getMetadata().getMatchId() == null) {
            return null;
        }
        String matchId = dto.getMetadata().getMatchId();
        if (valorantMatchDetailRepository.existsByMatchId(matchId)) {
            return null;
        }

        ValorantMatch match = valorantMatchMapper.toEntity(dto);
        return valorantMatchDetailRepository.save(match);
    }

    /**
     * 매치 DTO 목록을 DB에 저장 (중복 스킵)
     *
     * @param dtos 매치 DTO 목록
     * @param limit 저장할 최대 개수 (0이면 제한 없음)
     * @return 저장된 매치 수
     */
    @Transactional
    public int saveMatches(List<ValorantMatchDetailDto> dtos, int limit) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        int saved = 0;
        int max = limit > 0 ? Math.min(limit, dtos.size()) : dtos.size();

        for (int i = 0; i < max; i++) {
            ValorantMatchDetailDto dto = dtos.get(i);
            ValorantMatch savedMatch = saveMatch(dto);
            if (savedMatch != null) {
                saved++;
            }
        }
        return saved;
    }
}
