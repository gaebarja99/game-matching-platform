package com.gamematcher.service.account;

import com.gamematcher.dto.account.RiotAccountLinkRequestDto;
import com.gamematcher.dto.account.RiotAccountLinkResponseDto;
import com.gamematcher.dto.riot.RiotAccountResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.entity.account.RiotAccount;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.account.RiotAccountRepository;
import com.gamematcher.repository.common.CommonUserRepository;
import com.gamematcher.service.riot.LolApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiotAccountService {

    private final LolApiService lolApiService;
    private final RiotAccountRepository riotAccountRepository;
    private final CommonUserRepository userRepository;

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

            return new RiotAccountLinkResponseDto(
                    existing.getId(),
                    user.getId(),
                    existing.getPuuid(),
                    existing.getGameName(),
                    existing.getTagLine(),
                    existing.getGameType(),
                    existing.getVerificationMethod(),
                    existing.isOwnershipVerified(),
                    "\uC774\uBBF8 \uC778\uC99D\uB41C Riot \uACC4\uC815\uC785\uB2C8\uB2E4."
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

        return new RiotAccountLinkResponseDto(
                saved.getId(),
                saved.getUser().getId(),
                saved.getPuuid(),
                saved.getGameName(),
                saved.getTagLine(),
                saved.getGameType(),
                saved.getVerificationMethod(),
                saved.isOwnershipVerified(),
                "Riot \uACC4\uC815 \uC778\uC99D \uBC0F \uC5F0\uB3D9\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
        );
    }
}
