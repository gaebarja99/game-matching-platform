package com.gamematcher.repository;

import com.gamematcher.entity.PangCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PangChargeRepository extends JpaRepository<PangCharge, Long> {

    List<PangCharge> findByUserIdOrderByCreatedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    long countByUserId(Long userId);

    boolean existsByUserIdAndOrderIdAndPangAmountLessThan(Long userId, String orderId, Integer pangAmount);

    boolean existsByUserIdAndImpUidAndPangAmountLessThan(Long userId, String impUid, Integer pangAmount);
}
