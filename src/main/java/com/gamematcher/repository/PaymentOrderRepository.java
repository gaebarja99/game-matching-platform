package com.gamematcher.repository;

import com.gamematcher.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderId(String orderId);

    Optional<PaymentOrder> findByImpUid(String impUid);
}
