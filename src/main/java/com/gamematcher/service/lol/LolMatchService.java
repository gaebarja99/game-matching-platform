package com.gamematcher.service.lol;

import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.lol.LolMatchTimelineDetailDto;
import com.gamematcher.entity.match.lol.LolMatch;
import com.gamematcher.entity.match.lol.LolMatchTimeline;
import com.gamematcher.mapper.LolMatchMapper;
import com.gamematcher.repository.match.LolMatchRepository;
import com.gamematcher.repository.match.LolMatchTimelineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * LoL 매치 DB 저장 서비스
 */
@Service
public class LolMatchService {

    private final LolMatchRepository lolMatchRepository;
    private final LolMatchTimelineRepository lolMatchTimelineRepository;
    private final LolMatchMapper lolMatchMapper;

    public LolMatchService(LolMatchRepository lolMatchRepository,
                          LolMatchTimelineRepository lolMatchTimelineRepository,
                          LolMatchMapper lolMatchMapper) {
        this.lolMatchRepository = lolMatchRepository;
        this.lolMatchTimelineRepository = lolMatchTimelineRepository;
        this.lolMatchMapper = lolMatchMapper;
    }

    /**
     * 매치 DTO를 DB에 저장 (중복 시 스킵)
     *
     * @return 저장된 매치, 이미 존재하면 null
     */
    @Transactional
    public LolMatch saveMatch(LolMatchDetailDto dto) {
        if (dto == null || dto.getMetadata() == null || dto.getMetadata().getMatchId() == null) {
            return null;
        }
        String matchId = dto.getMetadata().getMatchId();
        if (lolMatchRepository.existsByMatchId(matchId)) {
            return null;
        }

        LolMatch match = lolMatchMapper.toMatchEntity(dto);
        match.setApiCachedAt(LocalDateTime.now());
        return lolMatchRepository.save(match);
    }

    /**
     * 전적 검색 캐시 갱신: 기존 행이 있으면 삭제 후 API DTO로 다시 저장한다.
     */
    @Transactional
    public LolMatch replaceMatchFromApi(LolMatchDetailDto dto) {
        if (dto == null || dto.getMetadata() == null || dto.getMetadata().getMatchId() == null) {
            return null;
        }
        String matchId = dto.getMetadata().getMatchId();
        lolMatchRepository.findByMatchId(matchId).ifPresent(lolMatchRepository::delete);
        LolMatch match = lolMatchMapper.toMatchEntity(dto);
        match.setApiCachedAt(LocalDateTime.now());
        return lolMatchRepository.save(match);
    }

    /**
     * 매치 + 타임라인 DTO를 DB에 저장
     *
     * @return 저장된 매치 (이미 존재하면 기존 매치 반환)
     */
    @Transactional
    public LolMatch saveMatchWithTimeline(LolMatchDetailDto matchDto, LolMatchTimelineDetailDto timelineDto) {
        LolMatch match = saveMatch(matchDto);
        if (match == null && matchDto != null && matchDto.getMetadata() != null) {
            match = lolMatchRepository.findByMatchId(matchDto.getMetadata().getMatchId()).orElse(null);
        }
        if (match == null) return null;

        if (timelineDto != null && match.getTimeline() == null) {
            LolMatchTimeline timeline = lolMatchMapper.toTimelineEntity(match, timelineDto);
            if (timeline != null) {
                lolMatchTimelineRepository.save(timeline);
                match.setTimeline(timeline);
            }
        }

        return match;
    }
}
