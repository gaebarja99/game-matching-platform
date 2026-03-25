package com.gamematcher.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DiscordAccountLinkRequestDto {

    @NotNull
    private Long userId;

    @NotBlank
    private String discordId;

    private String username;
    private String avatar;
}
