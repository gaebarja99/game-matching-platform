package com.gamematcher.dto.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {
    /** 충전할 팡 개수 */
    private Integer pangAmount;
}
