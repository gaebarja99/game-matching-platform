package com.gamematcher.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SteamAccountLinkRequestDto {

    @NotNull
    private Long userId;

    @NotBlank
    private String steamId;

    private String personaName;
    private String avatar;
}
