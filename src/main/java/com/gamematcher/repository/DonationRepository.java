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

    List<Donation> findByStreamIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long streamId,
            LocalDateTime createdAt,
            Pageable pageable
    );

    @Query("SELECT DISTINCT d.fromUserId FROM Donation d WHERE d.streamId = :streamId")
    List<Long> findDistinctFromUserIdByStreamId(@Param("streamId") Long streamId);

    boolean existsByFromUserIdAndStreamId(Long fromUserId, Long streamId);

    List<Donation> findByFromUserIdOrderByCreatedAtDesc(Long fromUserId, Pageable pageable);

    long countByFromUserId(Long fromUserId);

    List<Donation> findByFromUserIdAndToUserIdOrderByCreatedAtDesc(Long fromUserId, Long toUserId, Pageable pageable);

    @Query("SELECT d.toUserId, SUM(d.amount) FROM Donation d GROUP BY d.toUserId ORDER BY SUM(d.amount) DESC")
    List<Object[]> findTopUserIdsByTotalDonation(Pageable pageable);

    @Query("SELECT d.fromUserId, SUM(d.amount) FROM Donation d WHERE d.streamId = :streamId AND d.createdAt >= :weekStart GROUP BY d.fromUserId ORDER BY SUM(d.amount) DESC")
    List<Object[]> findWeeklyTopDonorsByStream(@Param("streamId") Long streamId, @Param("weekStart") LocalDateTime weekStart, Pageable pageable);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donation d WHERE d.toUserId = :toUserId")
    long sumAmountByToUserId(@Param("toUserId") Long toUserId);

    @Query("SELECT d.streamId, SUM(d.amount) FROM Donation d WHERE d.toUserId = :toUserId GROUP BY d.streamId ORDER BY SUM(d.amount) DESC")
    List<Object[]> findStreamIdAndSumByToUserId(@Param("toUserId") Long toUserId);
}
