package com.gamematcher.dto.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AccountConnectionsResponseDto {
    private Long userId;
    private List<AccountConnectionStatusDto> connections;
}
