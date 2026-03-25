package com.gamematcher.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Riot 계정 연동 요청 (user_id + gameName + tagLine)
 */
@Getter
@Setter
@NoArgsConstructor
public class RiotAccountLinkRequestDto {

    @NotNull(message = "user_id는 필수입니다")
    private Long userId;

    @NotBlank(message = "gameName은 필수입니다")
    private String gameName;

    @NotBlank(message = "tagLine은 필수입니다")
    private String tagLine;
}
