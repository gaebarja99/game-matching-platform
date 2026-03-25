package com.gamematcher.dto.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiotManualLinkRequestDto {

    @NotBlank
    private String gameName;

    @NotBlank
    private String tagLine;
}
