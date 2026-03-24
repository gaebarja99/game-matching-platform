package com.gamematcher.service;

import com.gamematcher.entity.UserPushToken;
import com.gamematcher.repository.UserPushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final UserPushTokenRepository userPushTokenRepository;

    @Transactional
    public void registerToken(Long userId, String token, String platform) {
        if (userId == null) return;
        if (token == null || token.isBlank()) return;

        UserPushToken entry = userPushTokenRepository.findByFcmToken(token).orElseGet(UserPushToken::new);
        entry.setUserId(userId);
        entry.setFcmToken(token.trim());
        entry.setPlatform((platform == null || platform.isBlank()) ? "web" : platform.trim());
        userPushTokenRepository.save(entry);
    }

    @Transactional
    public void unregisterToken(Long userId, String token) {
        if (userId == null || token == null || token.isBlank()) return;
        userPushTokenRepository.deleteByUserIdAndFcmToken(userId, token.trim());
    }

    @Transactional(readOnly = true)
    public List<String> findTokensByUserId(Long userId) {
        if (userId == null) return List.of();
        return userPushTokenRepository.findByUserId(userId).stream()
                .map(UserPushToken::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }
}

