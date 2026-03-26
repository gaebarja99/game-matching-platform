package com.gamematcher.dto.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiotVerificationStartRequestDto {

    @NotBlank
    private String gameType;

    private String platform;

    @NotBlank
    private String gameName;

    @NotBlank
    private String tagLine;
}
