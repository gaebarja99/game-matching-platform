package com.gamematcher.service.account;

import com.gamematcher.entity.User;
import com.gamematcher.entity.account.DiscordAccount;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.account.DiscordAccountRepository;
import com.gamematcher.repository.common.CommonUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Discord 계정 연동 및 DB 저장
 */
@Service
@RequiredArgsConstructor
public class DiscordAccountService {

    private final DiscordAccountRepository discordAccountRepository;
    private final CommonUserRepository userRepository;

    @Transactional
    public DiscordAccount linkDiscordAccount(Long userId, String discordId, String username, String avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId));

        if (discordAccountRepository.existsByDiscordId(discordId)) {
            DiscordAccount existing = discordAccountRepository.findByDiscordId(discordId).orElseThrow();
            if (existing.getUser().getId().equals(userId)) {
                return existing;
            }
            throw new GameApiException(HttpStatus.CONFLICT, "이 Discord 계정은 이미 다른 사용자에게 연동되어 있습니다.");
        }

        DiscordAccount account = new DiscordAccount();
        account.setUser(user);
        account.setDiscordId(discordId);
        account.setUsername(username);
        account.setAvatar(avatar);
        return discordAccountRepository.save(account);
    }
}
