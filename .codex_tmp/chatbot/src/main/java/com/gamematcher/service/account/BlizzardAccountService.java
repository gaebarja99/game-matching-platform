package com.gamematcher.service.account;

import com.gamematcher.entity.User;
import com.gamematcher.entity.account.BlizzardAccount;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.account.BlizzardAccountRepository;
import com.gamematcher.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blizzard Battle.net 계정 연동 및 DB 저장
 */
@Service
@RequiredArgsConstructor
public class BlizzardAccountService {

    private final BlizzardAccountRepository blizzardAccountRepository;
    private final UserRepository userRepository;

    @Transactional
    public BlizzardAccount linkBlizzardAccount(Long userId, String battleTag, String accountId, String region) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId));

        String reg = region != null ? region : "kr";
        if (blizzardAccountRepository.existsByAccountIdAndRegion(accountId, reg)) {
            BlizzardAccount existing = blizzardAccountRepository.findByAccountIdAndRegion(accountId, reg).orElseThrow();
            if (existing.getUser().getId().equals(userId)) {
                return existing;
            }
            throw new GameApiException(HttpStatus.CONFLICT, "이 Battle.net 계정은 이미 다른 사용자에게 연동되어 있습니다.");
        }

        BlizzardAccount account = new BlizzardAccount();
        account.setUser(user);
        account.setBattleTag(battleTag);
        account.setAccountId(accountId);
        account.setRegion(reg);
        return blizzardAccountRepository.save(account);
    }
}
