package com.gamematcher.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    /** 가맹점 주문번호 (merchant_uid). 결제창·검증 시 사용 */
    private String orderId;
    /** 결제 금액 (원) */
    private Long amount;
    /** 주문명 (결제창 표시) */
    private String orderName;
    /** 포트원 가맹점 식별자 (프론트 IMP.init용). 콘솔에서 확인 */
    private String storeId;
}
