package com.gamematcher.service.account;

import com.gamematcher.dto.account.RiotAccountLinkRequestDto;
import com.gamematcher.dto.account.RiotAccountLinkResponseDto;
import com.gamematcher.dto.riot.RiotAccountResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.entity.account.RiotAccount;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.account.RiotAccountRepository;
import com.gamematcher.repository.common.UserRepository;
import com.gamematcher.service.riot.LolApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RiotAccountService {

    private final LolApiService lolApiService;
    private final RiotAccountRepository riotAccountRepository;
    private final UserRepository userRepository;

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

        return new RiotAccountLinkResponseDto(
                saved.getId(),
                saved.getUser().getId(),
                saved.getPuuid(),
                saved.getGameName(),
                saved.getTagLine(),
                "Riot 계정 연동 완료"
        );
    }
}
