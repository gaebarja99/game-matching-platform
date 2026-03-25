package com.gamematcher.dto.pang;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChargeRequest {
    /** 충전할 팡 개수 (1 이상) */
    private Integer amount;
}
