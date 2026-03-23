package com.gamematcher.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlizzardAccountLinkRequestDto {

    @NotNull
    private Long userId;

    @NotBlank
    private String battleTag;

    @NotBlank
    private String accountId;

    private String region = "kr";
}
