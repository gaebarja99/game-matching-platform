package com.gamematcher.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccountConnectionStatusDto {
    private String provider;
    private boolean connected;
    private String displayName;
    private String secondaryValue;
    private String avatarUrl;
    private boolean ownershipVerified;
    private String connectUrl;
    private String note;
}
