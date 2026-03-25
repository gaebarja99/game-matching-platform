package com.gamematcher.service.account;

import com.gamematcher.exception.GameApiException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthLinkStateService {

    private final Map<String, PendingLinkState> states = new ConcurrentHashMap<>();

    public String createState(Long userId, String provider) {
        String state = UUID.randomUUID().toString().replace("-", "");
        states.put(state, new PendingLinkState(userId, provider, Instant.now().plusSeconds(600)));
        return state;
    }

    public PendingLinkState consumeState(String state, String provider) {
        PendingLinkState pending = states.remove(state);
        if (pending == null || !pending.getProvider().equalsIgnoreCase(provider) || pending.getExpiresAt().isBefore(Instant.now())) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "연동 요청 상태값이 유효하지 않습니다. 다시 시도해 주세요.");
        }
        return pending;
    }

    @Getter
    @AllArgsConstructor
    public static class PendingLinkState {
        private Long userId;
        private String provider;
        private Instant expiresAt;
    }
}
