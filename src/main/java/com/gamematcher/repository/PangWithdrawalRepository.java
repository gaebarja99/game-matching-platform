package com.gamematcher.repository;

import com.gamematcher.entity.PangWithdrawal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 스트리머 환전 신청 */
public interface PangWithdrawalRepository extends JpaRepository<PangWithdrawal, Long> {

    List<PangWithdrawal> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 해당 사용자가 이미 환전한 총량 (신청량 기준) */
    @Query("SELECT COALESCE(SUM(w.amountPang), 0) FROM PangWithdrawal w WHERE w.userId = :userId")
    long sumAmountPangByUserId(@Param("userId") Long userId);
}
