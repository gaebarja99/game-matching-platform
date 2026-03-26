package com.gamematcher.repository;

import com.gamematcher.constant.LolTier;
import com.gamematcher.entity.MatchingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchingQueueRepository extends JpaRepository<MatchingQueue, Long> {

    @Query("""
            select mq
            from MatchingQueue mq
            where mq.gameName = :gameName
              and mq.matched = false
              and mq.tier = :tier
            order by mq.createdAt asc
            """)
    List<MatchingQueue> findWaitingByGameAndTierOrderByCreatedAtAsc(
            @Param("gameName") String gameName,
            @Param("tier") LolTier tier
    );

    @Query("""
            select mq
            from MatchingQueue mq
            where mq.gameName = :gameName
              and mq.matched = false
              and mq.createdAt <= :beforeInclusive
            order by mq.createdAt asc
            """)
    List<MatchingQueue> findWaitingByGameAndCreatedAtBeforeOrderByCreatedAtAsc(
            @Param("gameName") String gameName,
            @Param("beforeInclusive") LocalDateTime beforeInclusive
    );

    @Query("""
            select count(mq)
            from MatchingQueue mq
            where mq.gameName = :gameName
              and mq.matched = false
              and mq.tier = :tier
            """)
    long countWaitingByGameAndTier(
            @Param("gameName") String gameName,
            @Param("tier") LolTier tier
    );

    boolean existsByUserId(Long userId);

    Optional<MatchingQueue> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
