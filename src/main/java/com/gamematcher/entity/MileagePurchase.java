package com.gamematcher.entity;

import com.gamematcher.constant.MileagePurchaseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mileage_purchases")
@Getter
@Setter
@NoArgsConstructor
public class MileagePurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private MileagePurchaseType type;

    /** 차감 마일리지(원 단위) */
    @Column(name = "mileage_cost", nullable = false)
    private Long mileageCost;

    /** 지급 팡(팡 상품 구매일 때만) */
    @Column(name = "pang_amount")
    private Integer pangAmount;

    /** 구독권 구매 대상 스트리머 ID (구독권 상품일 때만) */
    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
