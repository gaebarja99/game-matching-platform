package com.gamematcher.service.account;

import com.gamematcher.entity.User;
import com.gamematcher.entity.account.SteamAccount;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.account.SteamAccountRepository;
import com.gamematcher.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Steam 계정 연동 및 DB 저장
 */
@Service
@RequiredArgsConstructor
public class SteamAccountService {

    private final SteamAccountRepository steamAccountRepository;
    private final UserRepository userRepository;

    @Transactional
    public SteamAccount linkSteamAccount(Long userId, String steamId, String personaName, String avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId));

        if (steamAccountRepository.existsBySteamId(steamId)) {
            SteamAccount existing = steamAccountRepository.findBySteamId(steamId).orElseThrow();
            if (existing.getUser().getId().equals(userId)) {
                return existing;
            }
            throw new GameApiException(HttpStatus.CONFLICT, "이 Steam 계정은 이미 다른 사용자에게 연동되어 있습니다.");
        }

        SteamAccount account = new SteamAccount();
        account.setUser(user);
        account.setSteamId(steamId);
        account.setPersonaName(personaName);
        account.setAvatar(avatar);
        return steamAccountRepository.save(account);
    }
}
