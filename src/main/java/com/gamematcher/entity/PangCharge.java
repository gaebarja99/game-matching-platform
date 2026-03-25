package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pang_charges")
@Getter
@Setter
@NoArgsConstructor
public class PangCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 충전한 팡 수 */
    @Column(name = "pang_amount", nullable = false)
    private Integer pangAmount;

    /** 결제 금액 (원). 1팡 = 1.2원. long 사용(대량 충전 시 int 오버플로우 방지) */
    @Column(name = "price_won", nullable = false)
    private Long priceWon;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "imp_uid", length = 100)
    private String impUid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
