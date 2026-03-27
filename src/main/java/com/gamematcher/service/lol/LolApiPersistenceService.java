package com.gamematcher.service.lol;

import com.gamematcher.dto.lol.*;
import com.gamematcher.entity.account.LolAccount;
import com.gamematcher.entity.account.LolSummonerProfile;
import com.gamematcher.entity.match.lol.LolMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * LoL API 응답 DTO → 엔티티 저장 통합 서비스
 * Riot API 응답을 DB에 저장
 */
@Service
@RequiredArgsConstructor
public class LolApiPersistenceService {

    private final LolAccountService lolAccountService;
    private final LolSummonerProfileService lolSummonerProfileService;
    private final LolMatchService lolMatchService;

    /**
     * 계정 DTO 저장 (LolAccountResponseDto → LolAccount)
     *
     * @param dto Riot Account-v1 API 응답
     * @return 저장된 LolAccount, 실패 시 null
     */
    @Transactional
    public LolAccount saveAccount(LolAccountResponseDto dto) {
        return lolAccountService.saveAccount(dto);
    }

    /**
     * 소환사 프로필 DTO 저장 (LolSummonerProfileDto → LolSummonerProfile)
     *
     * @param dto Riot Summoner-v4 API 응답
     * @return 저장된 LolSummonerProfile, 실패 시 null
     */
    @Transactional
    public LolSummonerProfile saveSummonerProfile(LolSummonerProfileDto dto) {
        return lolSummonerProfileService.saveProfile(dto);
    }

    /**
     * 매치 DTO 저장 (LolMatchDetailDto → LolMatch)
     *
     * @param dto Riot Match-v5 API 응답
     * @return 저장된 LolMatch (이미 존재하면 null)
     */
    @Transactional
    public LolMatch saveMatch(LolMatchDetailDto dto) {
        return lolMatchService.saveMatch(dto);
    }

    /**
     * 매치 + 타임라인 DTO 저장
     *
     * @param matchDto  Riot Match-v5 API 응답
     * @param timelineDto Riot Match-v5 Timeline API 응답
     * @return 저장된 LolMatch
     */
    @Transactional
    public LolMatch saveMatchWithTimeline(LolMatchDetailDto matchDto, LolMatchTimelineDetailDto timelineDto) {
        return lolMatchService.saveMatchWithTimeline(matchDto, timelineDto);
    }

    /**
     * 매치 ID 목록 기반으로 매치 상세 저장 (DTO는 별도 조회 필요)
     * 매치 ID만 있고 상세 DTO가 있을 때 각각 saveMatch 호출
     *
     * @param matchDtos 매치 상세 DTO 목록
     * @return 저장된 매치 목록 (이미 존재한 것은 제외)
     */
    @Transactional
    public List<LolMatch> saveMatches(List<LolMatchDetailDto> matchDtos) {
        List<LolMatch> saved = new ArrayList<>();
        if (matchDtos == null) return saved;

        for (LolMatchDetailDto dto : matchDtos) {
            LolMatch m = lolMatchService.saveMatch(dto);
            if (m != null) {
                saved.add(m);
            }
        }
        return saved;
    }

    /**
     * 계정 + 프로필 한 번에 저장
     * Riot ID로 계정 조회 후, puuid로 소환사 프로필 조회한 흐름에 적합
     *
     * @param accountDto Riot Account-v1 응답
     * @param profileDto Riot Summoner-v4 응답 (같은 puuid)
     * @return [LolAccount, LolSummonerProfile]
     */
    @Transactional
    public LolAccountAndProfile saveAccountAndProfile(LolAccountResponseDto accountDto,
                                                      LolSummonerProfileDto profileDto) {
        LolAccount account = lolAccountService.saveAccount(accountDto);
        LolSummonerProfile profile = profileDto != null ? lolSummonerProfileService.saveProfile(profileDto) : null;
        return new LolAccountAndProfile(account, profile);
    }

    /** 계정 + 프로필 저장 결과 */
    public record LolAccountAndProfile(LolAccount account, LolSummonerProfile profile) {}
}
