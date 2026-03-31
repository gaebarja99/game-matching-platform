package com.gamematcher.repository;

import com.gamematcher.constant.PaymentOrderKind;
import com.gamematcher.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderId(String orderId);

    Optional<PaymentOrder> findByImpUid(String impUid);

    @Query("""
            SELECT COALESCE(SUM(p.amountWon), 0)
            FROM PaymentOrder p
            WHERE p.kind = :kind
              AND p.targetUserId = :targetUserId
              AND p.status = 'COMPLETED'
            """)
    long sumCompletedAmountWonByKindAndTargetUserId(@Param("kind") PaymentOrderKind kind, @Param("targetUserId") Long targetUserId);
}
