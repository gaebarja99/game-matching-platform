package com.gamematcher.repository;

import com.gamematcher.entity.PangWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 스트리머 팡 환전 신청 */
public interface PangWithdrawalRepository extends JpaRepository<PangWithdrawal, Long> {

    /** 해당 사용자가 이미 환전한 총 팡 (신청액 기준) */
    @Query("SELECT COALESCE(SUM(w.amountPang), 0) FROM PangWithdrawal w WHERE w.userId = :userId")
    long sumAmountPangByUserId(@Param("userId") Long userId);
}
