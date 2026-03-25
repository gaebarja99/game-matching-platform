package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 스트리머 팡 환전 신청 (수수료 차감 후 정산액 기록) */
@Entity
@Table(name = "pang_withdrawals")
@Getter
@Setter
@NoArgsConstructor
public class PangWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 신청 팡 (수수료 차감 전) */
    @Column(name = "amount_pang", nullable = false)
    private Long amountPang;

    /** 수수료율 (예: 30) */
    @Column(name = "commission_percent", nullable = false)
    private Integer commissionPercent;

    /** 수수료 팡 */
    @Column(name = "commission_pang", nullable = false)
    private Long commissionPang;

    /** 정산 팡 (신청액 - 수수료) */
    @Column(name = "net_pang", nullable = false)
    private Long netPang;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
