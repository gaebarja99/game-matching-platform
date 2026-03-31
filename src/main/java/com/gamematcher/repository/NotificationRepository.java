package com.gamematcher.repository;

import com.gamematcher.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUserIdAndReadAtIsNull(Long userId);

    long countByUserIdAndTypeAndReadAtIsNull(Long userId, String type);

    long countByUserIdAndTypeAndActorUserIdAndReadAtIsNull(Long userId, String type, Long actorUserId);

    List<Notification> findByUserIdAndTypeAndActorUserIdAndReadAtIsNull(Long userId, String type, Long actorUserId);

    boolean existsByUserIdAndTypeAndCreatedAt(Long userId, String type, java.time.LocalDateTime createdAt);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
