package com.gamematcher.dto.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConfirmPaymentRequest {
    /** 가맹점 주문번호 (merchant_uid) */
    private String orderId;
    /** 포트원 결제 고유번호 (imp_uid) */
    private String impUid;
    /** 일부 SDK 호환용 결제 ID (paymentId/payment_id) */
    private String paymentId;

    public String resolveImpUid() {
        if (impUid != null && !impUid.isBlank()) return impUid;
        if (paymentId != null && !paymentId.isBlank()) return paymentId;
        return null;
    }
}
