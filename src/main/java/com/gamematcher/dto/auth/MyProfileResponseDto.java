package com.gamematcher.dto.auth;

import com.gamematcher.constant.UserStatus;
import com.gamematcher.dto.account.AccountConnectionStatusDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyProfileResponseDto {
    private Long userId;
    private String loginId;
    private String username;
    private String email;
    private String role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private List<AccountConnectionStatusDto> connections;
}
