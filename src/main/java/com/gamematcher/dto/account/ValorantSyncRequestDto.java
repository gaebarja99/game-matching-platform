package com.gamematcher.dto.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ValorantSyncRequestDto {

    @NotBlank
    private String gameName;

    @NotBlank
    private String tagLine;

    private String region = "kr"; // 기본값 kr
}
