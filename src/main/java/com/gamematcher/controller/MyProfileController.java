package com.gamematcher.controller;

import com.gamematcher.dto.account.AccountConnectionsResponseDto;
import com.gamematcher.dto.auth.MyProfileResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.service.account.AccountConnectionService;
import com.gamematcher.service.auth.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyProfileController {

    private final CurrentUserService currentUserService;
    private final AccountConnectionService accountConnectionService;

    @GetMapping
    public MyProfileResponseDto getMyProfile(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken) {
        User user = currentUserService.requireUser(authToken);
        AccountConnectionsResponseDto connections = accountConnectionService.getConnections(authToken);

        return new MyProfileResponseDto(
                user.getId(),
                user.getLoginId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus(),
                user.getCreatedAt(),
                connections.getConnections()
        );
    }
}
