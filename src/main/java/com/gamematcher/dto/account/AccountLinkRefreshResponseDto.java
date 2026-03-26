package com.gamematcher.dto.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 연동 계정 「정보 불러오기」 응답 */
@Getter
@AllArgsConstructor
public class AccountLinkRefreshResponseDto {
    /** 서버에 저장된 표시 정보가 바뀌었는지 (Steam 등) */
    private boolean updated;
    private String message;
}
