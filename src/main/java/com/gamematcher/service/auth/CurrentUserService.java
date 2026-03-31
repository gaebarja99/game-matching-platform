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
}
