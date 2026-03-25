package com.gamematcher.repository;

import com.gamematcher.entity.MileagePurchase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MileagePurchaseRepository extends JpaRepository<MileagePurchase, Long> {

    List<MileagePurchase> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
