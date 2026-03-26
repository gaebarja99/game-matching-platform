package com.gamematcher.service.auth;

import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.CommonUserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String SESSION_USER_ID = "userId";

    private final CommonUserRepository userRepository;

    public User requireUser(String authToken) {
        if (authToken != null && !authToken.isBlank()) {
            return userRepository.findByAuthToken(authToken)
                    .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "로그인 세션이 유효하지 않습니다."));
        }

        Long sessionUserId = resolveSessionUserId();
        if (sessionUserId != null) {
            return userRepository.findById(sessionUserId)
                    .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "로그인 세션이 유효하지 않습니다."));
        }

        throw new GameApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }

    private Long resolveSessionUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }

        HttpSession session = servletAttributes.getRequest().getSession(false);
        if (session == null) {
            return null;
        }

        Object userId = session.getAttribute(SESSION_USER_ID);
        if (userId instanceof Long longUserId) {
            return longUserId;
        }
        if (userId instanceof Integer intUserId) {
            return intUserId.longValue();
        }
        return null;
    }

    /**
     * HTTP 세션의 {@code userId}를 우선 사용하고, 세션에 로그인 정보가 없을 때만 {@code X-Auth-Token}으로 조회합니다.
     * 브라우저는 쿠키 세션으로 로그인하는데, 잘못된 토큰 헤더가 붙는 경우(확장 프로그램 등)에도 세션이 유효하면 동작하게 합니다.
     */
    public User requireUserByTokenOrSession(String authToken, HttpSession session) {
        Optional<Long> sessionUserId = userIdFromSession(session);
        if (sessionUserId.isPresent()) {
            return userRepository.findById(sessionUserId.get())
                    .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "로그인 세션이 유효하지 않습니다."));
        }
        if (authToken != null && !authToken.isBlank()) {
            return userRepository.findByAuthToken(authToken.trim())
                    .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "로그인 세션이 유효하지 않습니다."));
        }
        throw new GameApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }

    /**
     * 선택적 인증: 세션 우선, 없으면 토큰. 없거나 무효하면 empty.
     */
    public Optional<Long> tryCurrentUserId(String authToken, HttpSession session) {
        Optional<Long> sessionUserId = userIdFromSession(session);
        if (sessionUserId.isPresent()) {
            return userRepository.findById(sessionUserId.get()).map(User::getId);
        }
        if (authToken != null && !authToken.isBlank()) {
            return userRepository.findByAuthToken(authToken.trim()).map(User::getId);
        }
        return Optional.empty();
    }

    private static Optional<Long> userIdFromSession(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object raw = session.getAttribute("userId");
        if (raw instanceof Long l) {
            return Optional.of(l);
        }
        if (raw instanceof Number n) {
            return Optional.of(n.longValue());
        }
        return Optional.empty();
    }
}
