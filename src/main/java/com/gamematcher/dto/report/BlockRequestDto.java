package com.gamematcher.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlockRequestDto {

    @NotNull(message = "차단할 사용자 ID는 필수입니다.")
    private Long blockedUserId;
}
