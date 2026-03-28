package com.gamematcher.repository;

import com.gamematcher.constant.MileagePurchaseType;
import com.gamematcher.entity.MileagePurchase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MileagePurchaseRepository extends JpaRepository<MileagePurchase, Long> {

    List<MileagePurchase> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<MileagePurchase> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, MileagePurchaseType type, Pageable pageable);

    long countByUserId(Long userId);

    boolean existsByUserIdAndTypeAndMileageCostAndCreatedAt(
            Long userId,
            MileagePurchaseType type,
            Long mileageCost,
            java.time.LocalDateTime createdAt
    );

    @Query("""
            SELECT COALESCE(SUM(m.mileageCost), 0)
            FROM MileagePurchase m
            WHERE m.type = :type
              AND m.targetUserId = :targetUserId
            """)
    long sumMileageCostByTypeAndTargetUserId(@Param("type") MileagePurchaseType type, @Param("targetUserId") Long targetUserId);
}
