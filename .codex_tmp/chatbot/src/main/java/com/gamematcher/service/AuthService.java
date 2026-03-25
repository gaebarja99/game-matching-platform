package com.gamematcher.service;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.dto.auth.AuthDto;
import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthDto.LoginResponse signup(AuthDto.SignupRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new GameApiException(HttpStatus.CONFLICT, "?대? ?ъ슜 以묒씤 ?꾩씠?붿엯?덈떎.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GameApiException(HttpStatus.CONFLICT, "?대? ?ъ슜 以묒씤 ?대찓?쇱엯?덈떎.");
        }

        User user = new User();
        user.setLoginId(request.getLoginId());
        user.setPassword(request.getPassword());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        issueAuthToken(user);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new GameApiException(HttpStatus.UNAUTHORIZED, "?꾩씠???먮뒗 鍮꾨?踰덊샇媛 ?щ컮瑜댁? ?딆뒿?덈떎."));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "?뺤???怨꾩젙?낅땲??");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "?덊눜??怨꾩젙?낅땲??");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            throw new GameApiException(HttpStatus.UNAUTHORIZED, "?꾩씠???먮뒗 鍮꾨?踰덊샇媛 ?щ컮瑜댁? ?딆뒿?덈떎.");
        }

        issueAuthToken(user);
        user.setLastLoginAt(LocalDateTime.now());
        return toResponse(user);
    }

    private AuthDto.LoginResponse toResponse(User user) {
        return new AuthDto.LoginResponse(
                user.getId(),
                user.getLoginId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getAuthToken()
        );
    }

    private void issueAuthToken(User user) {
        user.setAuthToken(UUID.randomUUID().toString().replace("-", ""));
        user.setAuthTokenIssuedAt(LocalDateTime.now());
    }
}
