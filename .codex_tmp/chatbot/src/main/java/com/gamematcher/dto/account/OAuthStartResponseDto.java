package com.gamematcher.dto.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthStartResponseDto {
    private String provider;
    private String authorizationUrl;
}
