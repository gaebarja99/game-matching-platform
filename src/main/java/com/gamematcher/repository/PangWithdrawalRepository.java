package com.gamematcher.repository;

import com.gamematcher.entity.PangWithdrawal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PangWithdrawalRepository extends JpaRepository<PangWithdrawal, Long> {

    List<PangWithdrawal> findByUserIdOrderByCreatedAtAsc(Long userId);

    List<PangWithdrawal> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(w.amountPang), 0) FROM PangWithdrawal w WHERE w.userId = :userId")
    long sumAmountPangByUserId(@Param("userId") Long userId);
}
