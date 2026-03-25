package com.gamematcher.service.auth;

import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.CommonUserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final CommonUserRepository userRepository;

    public User requireUser(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            throw new GameApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findByAuthToken(authToken)
                .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "로그인 세션이 유효하지 않습니다."));
    }

    /**
     * {@code X-Auth-Token}이 있으면 토큰으로 사용자 조회, 없으면 HTTP 세션의 {@code userId}로 조회
     * ({@link com.gamematcher.controller.AuthController} 로그인 세션과 동일).
     */
    public User requireUserByTokenOrSession(String authToken, HttpSession session) {
        if (authToken != null && !authToken.isBlank()) {
            return userRepository.findByAuthToken(authToken.trim())
                    .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "로그인 세션이 유효하지 않습니다."));
        }
        if (session == null) {
            throw new GameApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Object raw = session.getAttribute("userId");
        Long userId = null;
        if (raw instanceof Long l) {
            userId = l;
        } else if (raw instanceof Number n) {
            userId = n.longValue();
        }
        if (userId == null) {
            throw new GameApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "로그인 세션이 유효하지 않습니다."));
    }

    /**
     * 선택적 인증: 토큰 또는 세션으로 현재 사용자 id. 없거나 무효하면 empty.
     */
    public Optional<Long> tryCurrentUserId(String authToken, HttpSession session) {
        if (authToken != null && !authToken.isBlank()) {
            return userRepository.findByAuthToken(authToken.trim()).map(User::getId);
        }
        if (session == null) {
            return Optional.empty();
        }
        Object raw = session.getAttribute("userId");
        Long userId = null;
        if (raw instanceof Long l) {
            userId = l;
        } else if (raw instanceof Number n) {
            userId = n.longValue();
        }
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId).map(User::getId);
    }
}
