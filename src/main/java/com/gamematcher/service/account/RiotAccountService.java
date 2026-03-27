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

import java.time.LocalDateTime;
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
        return linkVerifiedAccount(userId, "lol", gameName, tagLine, null, "MANUAL_INPUT");
    }

    @Transactional
    public RiotAccountLinkResponseDto linkVerifiedAccount(
            Long userId,
            String gameType,
            String gameName,
            String tagLine,
            String verifiedPuuid,
            String verificationMethod
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "\uC0AC\uC6A9\uC790\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4: " + userId));

        RiotAccountResponseDto riotAccount = lolApiService.getAccountByRiotId(gameName, tagLine);
        String puuid = verifiedPuuid != null && !verifiedPuuid.isBlank() ? verifiedPuuid : riotAccount.getPuuid();

        if (verifiedPuuid != null && !verifiedPuuid.isBlank() && !verifiedPuuid.equals(riotAccount.getPuuid())) {
            throw new GameApiException(HttpStatus.CONFLICT, "\uC778\uC99D\uD55C Riot \uACC4\uC815 \uC815\uBCF4\uC640 \uD604\uC7AC \uC870\uD68C\uB41C \uACC4\uC815 \uC815\uBCF4\uAC00 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");
        }

        RiotAccount existing = riotAccountRepository.findByPuuid(puuid).orElse(null);
        if (existing != null) {
            if (!existing.getUser().getId().equals(userId)) {
                throw new GameApiException(HttpStatus.CONFLICT, "\uC774\uBBF8 \uB2E4\uB978 \uC0AC\uC6A9\uC790\uC5D0\uAC8C \uC5F0\uB3D9\uB41C Riot \uACC4\uC815\uC785\uB2C8\uB2E4.");
            }

            existing.setGameName(riotAccount.getGameName());
            existing.setTagLine(riotAccount.getTagLine());
            existing.setGameType(gameType);
            existing.setVerificationMethod(verificationMethod);
            existing.setOwnershipVerified(true);
            existing.setVerifiedAt(LocalDateTime.now());
            riotAccountRepository.save(existing);
            syncLolAndValorantFromRiotAccount(riotAccount);

            return new RiotAccountLinkResponseDto(
                    existing.getId(),
                    user.getId(),
                    existing.getPuuid(),
                    existing.getGameName(),
                    existing.getTagLine(),
                    existing.getGameType(),
                    existing.getVerificationMethod(),
                    existing.isOwnershipVerified(),
                    "\uC774\uBBF8 \uC778\uC99D\uB41C Riot \uACC4\uC815\uC785\uB2C8\uB2E4. \uCD5C\uC2E0 \uC804\uC801\uC744 \uBC18\uC601\uD588\uC2B5\uB2C8\uB2E4."
            );
        }

        RiotAccount entity = new RiotAccount();
        entity.setUser(user);
        entity.setPuuid(puuid);
        entity.setGameName(riotAccount.getGameName());
        entity.setTagLine(riotAccount.getTagLine());
        entity.setGameType(gameType);
        entity.setVerificationMethod(verificationMethod);
        entity.setOwnershipVerified(true);
        entity.setVerifiedAt(LocalDateTime.now());

        RiotAccount saved = riotAccountRepository.save(entity);
        syncLolAndValorantFromRiotAccount(riotAccount);

        return new RiotAccountLinkResponseDto(
                saved.getId(),
                saved.getUser().getId(),
                saved.getPuuid(),
                saved.getGameName(),
                saved.getTagLine(),
                saved.getGameType(),
                saved.getVerificationMethod(),
                saved.isOwnershipVerified(),
                "Riot \uACC4\uC815 \uC778\uC99D \uBC0F \uC5F0\uB3D9\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. LoL\u00B7\uBC1C\uB85C\uB79C\uD2B8 \uAE30\uBCF8 \uC804\uC801\uC744 \uBD88\uB7EC\uC654\uC2B5\uB2C8\uB2E4."
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
