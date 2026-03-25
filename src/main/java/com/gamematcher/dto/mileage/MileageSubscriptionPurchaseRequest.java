package com.gamematcher.dto.mileage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MileageSubscriptionPurchaseRequest {
    /** 구독 대상 스트리머 userId */
    private Long userId;
}
