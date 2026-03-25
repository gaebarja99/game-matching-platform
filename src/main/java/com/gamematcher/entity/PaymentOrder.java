package com.gamematcher.entity;

import com.gamematcher.constant.PaymentOrderKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 결제 주문. merchant_uid = orderId, imp_uid = 포트원 결제 고유번호. */
@Entity
@Table(name = "payment_orders", indexes = { @Index(name = "idx_payment_order_id", columnList = "order_id") })
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 가맹점 주문번호 (포트원 merchant_uid). 결제창·검증 시 사용 */
    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 주문 종류: 팡 충전 / 구독 결제 */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private PaymentOrderKind kind = PaymentOrderKind.PANG_CHARGE;

    /** 구독 결제 대상 스트리머 ID (팡 충전은 null) */
    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "pang_amount", nullable = false)
    private Integer pangAmount;

    @Column(name = "amount_won", nullable = false)
    private Long amountWon;

    /** PENDING | COMPLETED | FAILED | CANCELLED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    /** 포트원 결제 고유번호 (imp_uid). 검증 후 저장 */
    @Column(name = "imp_uid", length = 100)
    private String impUid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
