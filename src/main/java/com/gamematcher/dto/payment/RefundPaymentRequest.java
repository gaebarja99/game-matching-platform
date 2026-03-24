package com.gamematcher.dto.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefundPaymentRequest {
    private String orderId;
    private String impUid;
    private String reason;
}
