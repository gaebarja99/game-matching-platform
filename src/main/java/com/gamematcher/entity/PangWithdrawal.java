package com.gamematcher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    @Column(name = "amount_pang", nullable = false)
    private Long amountPang;

    @Column(name = "commission_percent", nullable = false)
    private Integer commissionPercent;

    @Column(name = "commission_pang", nullable = false)
    private Long commissionPang;

    @Column(name = "donation_pang_used")
    private Long donationPangUsed;

    @Column(name = "subscription_pang_used")
    private Long subscriptionPangUsed;

    @Column(name = "net_pang", nullable = false)
    private Long netPang;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
