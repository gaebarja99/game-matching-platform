package com.gamematcher.repository;

import com.gamematcher.entity.Donation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByStreamIdOrderByCreatedAtDesc(Long streamId, Pageable pageable);

    /** 해당 방송에 후원한 적 있는 사용자 ID 목록 (중복 제거) */
    @Query("SELECT DISTINCT d.fromUserId FROM Donation d WHERE d.streamId = :streamId")
    List<Long> findDistinctFromUserIdByStreamId(@Param("streamId") Long streamId);

    /** 해당 사용자가 해당 방송에 후원한 적이 있는지 */
    boolean existsByFromUserIdAndStreamId(Long fromUserId, Long streamId);

    /** 내가 후원한 내역 (사용 내역) */
    List<Donation> findByFromUserIdOrderByCreatedAtDesc(Long fromUserId, Pageable pageable);

    long countByFromUserId(Long fromUserId);

    long countByStreamId(Long streamId);

    /** 연속후원 일수 계산용: 후원자→스트리머 후원 내역 최신순 */
    List<Donation> findByFromUserIdAndToUserIdOrderByCreatedAtDesc(Long fromUserId, Long toUserId, Pageable pageable);

    /** 팡(후원) 순위: 받은 후원 합계 많은 순 (toUserId, sum(amount)) */
    @Query("SELECT d.toUserId, SUM(d.amount) FROM Donation d GROUP BY d.toUserId ORDER BY SUM(d.amount) DESC")
    List<Object[]> findTopUserIdsByTotalDonation(Pageable pageable);

    /** 주간 후원 랭킹: 해당 스트림에 이번 주(월요일 0시~) 후원한 사용자별 합계, 상위 N명 */
    @Query("SELECT d.fromUserId, SUM(d.amount) FROM Donation d WHERE d.streamId = :streamId AND d.createdAt >= :weekStart GROUP BY d.fromUserId ORDER BY SUM(d.amount) DESC")
    List<Object[]> findWeeklyTopDonorsByStream(@Param("streamId") Long streamId, @Param("weekStart") LocalDateTime weekStart, Pageable pageable);

    /** 해당 사용자(스트리머)가 받은 총 후원 팡 */
    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donation d WHERE d.toUserId = :toUserId")
    long sumAmountByToUserId(@Param("toUserId") Long toUserId);

    /** 스트리머가 방송별로 받은 팡 합계 (streamId, sum(amount)) - toUserId 기준 */
    @Query("SELECT d.streamId, SUM(d.amount) FROM Donation d WHERE d.toUserId = :toUserId GROUP BY d.streamId ORDER BY SUM(d.amount) DESC")
    List<Object[]> findStreamIdAndSumByToUserId(@Param("toUserId") Long toUserId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donation d WHERE d.streamId = :streamId")
    long sumAmountByStreamId(@Param("streamId") Long streamId);

    @Query("""
            SELECT d.fromUserId, COUNT(d), COALESCE(SUM(d.amount), 0), MAX(d.createdAt)
            FROM Donation d
            WHERE d.toUserId = :toUserId
            GROUP BY d.fromUserId
            ORDER BY COALESCE(SUM(d.amount), 0) DESC, MAX(d.createdAt) DESC
            """)
    List<Object[]> summarizeFansByToUserId(@Param("toUserId") Long toUserId);
}
