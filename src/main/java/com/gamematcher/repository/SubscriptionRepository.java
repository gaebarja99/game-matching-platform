package com.gamematcher.repository;

import com.gamematcher.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByStreamerIdAndSubscriberId(Long streamerId, Long subscriberId);

    Optional<Subscription> findByStreamerIdAndSubscriberId(Long streamerId, Long subscriberId);

    void deleteByStreamerIdAndSubscriberId(Long streamerId, Long subscriberId);

    long countByStreamerId(Long streamerId);

    /** 해당 스트리머를 구독한 사용자 목록, 최신순 */
    List<Subscription> findByStreamerIdOrderByCreatedAtDesc(Long streamerId);
}
