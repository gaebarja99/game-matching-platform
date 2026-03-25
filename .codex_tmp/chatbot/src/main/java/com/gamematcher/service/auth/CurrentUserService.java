package com.gamematcher.service.auth;

import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireUser(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            throw new GameApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findByAuthToken(authToken)
                .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "로그인 세션이 유효하지 않습니다."));
    }
}
