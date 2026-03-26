package com.gamematcher.service.account;

import com.gamematcher.dto.account.RiotAccountLinkRequestDto;
import com.gamematcher.dto.account.RiotAccountLinkResponseDto;
import com.gamematcher.dto.lol.LolAccountResponseDto;
import com.gamematcher.dto.lol.LolSummonerProfileDto;
import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.dto.riot.RiotAccountResponseDto;
import com.gamematcher.dto.riot.RiotSummonerResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.entity.account.RiotAccount;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.account.RiotAccountRepository;
import com.gamematcher.repository.common.CommonUserRepository;
import com.gamematcher.service.lol.LolAccountService;
import com.gamematcher.service.lol.LolSummonerProfileService;
import com.gamematcher.service.riot.LolApiService;
import com.gamematcher.service.valorant.ValorantApiService;
import com.gamematcher.service.valorant.ValorantMmrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiotAccountService {

    private final LolApiService lolApiService;
    private final RiotAccountRepository riotAccountRepository;
    private final CommonUserRepository userRepository;
    private final LolAccountService lolAccountService;
    private final LolSummonerProfileService lolSummonerProfileService;
    private final ValorantApiService valorantApiService;
    private final ValorantMmrService valorantMmrService;

    @Transactional
    public RiotAccountLinkResponseDto linkAccount(RiotAccountLinkRequestDto request) {
        return linkAccount(request.getUserId(), request.getGameName(), request.getTagLine());
    }

    @Transactional
    public RiotAccountLinkResponseDto linkAccount(Long userId, String gameName, String tagLine) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId));

        RiotAccountResponseDto riotAccount = lolApiService.getAccountByRiotId(gameName, tagLine);

        if (riotAccountRepository.existsByPuuid(riotAccount.getPuuid())) {
            RiotAccount existing = riotAccountRepository.findByPuuid(riotAccount.getPuuid()).orElseThrow();
            if (existing.getUser().getId().equals(userId)) {
                syncLolAndValorantFromRiotAccount(riotAccount);
                return new RiotAccountLinkResponseDto(
                        existing.getId(),
                        user.getId(),
                        existing.getPuuid(),
                        existing.getGameName(),
                        existing.getTagLine(),
                        "이미 연동된 Riot 계정입니다."
                );
            }
            throw new GameApiException(HttpStatus.CONFLICT, "이미 다른 사용자에게 연동된 Riot 계정입니다.");
        }

        RiotAccount entity = new RiotAccount();
        entity.setUser(user);
        entity.setPuuid(riotAccount.getPuuid());
        entity.setGameName(riotAccount.getGameName());
        entity.setTagLine(riotAccount.getTagLine());

        RiotAccount saved = riotAccountRepository.save(entity);
        syncLolAndValorantFromRiotAccount(riotAccount);

        return new RiotAccountLinkResponseDto(
                saved.getId(),
                saved.getUser().getId(),
                saved.getPuuid(),
                saved.getGameName(),
                saved.getTagLine(),
                "Riot 계정 연동 완료. LoL·발로란트 기본 전적을 불러왔습니다."
        );
    }

    /**
     * 연동된 Riot 계정의 최신 게임명·태그를 반영하고 LoL·발로란트 캐시를 다시 동기화한다.
     */
    @Transactional
    public void refreshLinkedData(Long userId) {
        RiotAccount acc = riotAccountRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "연동된 Riot 계정이 없습니다."));
        RiotAccountResponseDto fresh = lolApiService.getAccountByPuuid(acc.getPuuid());
        if (!Objects.equals(acc.getPuuid(), fresh.getPuuid())) {
            throw new GameApiException(HttpStatus.CONFLICT, "Riot 계정 식별 정보가 일치하지 않습니다. 연동을 해제한 뒤 다시 연결해 주세요.");
        }
        acc.setGameName(fresh.getGameName());
        acc.setTagLine(fresh.getTagLine());
        riotAccountRepository.save(acc);
        syncLolAndValorantFromRiotAccount(fresh);
    }

    /**
     * Riot Account API로 받은 동일 계정으로 LoL(local DB)·발로란트(Henrik) 데이터를 조회·저장한다.
     */
    private void syncLolAndValorantFromRiotAccount(RiotAccountResponseDto account) {
        if (account == null || account.getPuuid() == null || account.getPuuid().isBlank()) {
            return;
        }
        String puuid = account.getPuuid();
        String gameName = account.getGameName() != null ? account.getGameName() : "";
        String tagLine = account.getTagLine() != null ? account.getTagLine() : "";

        try {
            LolAccountResponseDto accDto = new LolAccountResponseDto();
            accDto.setPuuid(puuid);
            accDto.setGameName(gameName);
            accDto.setTagLine(tagLine);
            lolAccountService.saveAccount(accDto);
        } catch (Exception e) {
            log.warn("Riot 연동 후 LoL 계정 저장 실패: {}", e.getMessage());
        }

        try {
            RiotSummonerResponseDto summoner = lolApiService.getSummonerByPuuid(puuid);
            if (summoner != null && summoner.getPuuid() != null) {
                LolSummonerProfileDto prof = new LolSummonerProfileDto();
                prof.setPuuid(summoner.getPuuid());
                prof.setProfileIconId(summoner.getProfileIconId());
                prof.setRevisionDate(summoner.getRevisionDate());
                prof.setSummonerLevel((int) Math.min(Integer.MAX_VALUE, summoner.getSummonerLevel()));
                lolSummonerProfileService.saveProfile(prof);
            }
        } catch (Exception e) {
            log.warn("Riot 연동 후 LoL 소환사 프로필 저장 실패: {}", e.getMessage());
        }

        try {
            if (!gameName.isBlank() && !tagLine.isBlank()) {
                valorantApiService.getAccountByNameTag(gameName, tagLine, false);
            }
        } catch (Exception e) {
            log.debug("Riot 연동 후 발로란트 계정 조회 생략: {}", e.getMessage());
        }

        try {
            if (!gameName.isBlank() && !tagLine.isBlank()) {
                ValorantMmrApiResponse mmr = valorantApiService.fetchMmrForRiotLinkedProfile(gameName, tagLine);
                if (mmr != null) {
                    valorantMmrService.saveMmr(mmr);
                }
            }
        } catch (Exception e) {
            log.debug("Riot 연동 후 발로란트 MMR 저장 생략: {}", e.getMessage());
        }
    }
}
