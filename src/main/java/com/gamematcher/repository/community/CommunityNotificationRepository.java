package com.gamematcher.repository.community;

import com.gamematcher.entity.community.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 커뮤니티 알림. {@link com.gamematcher.repository.NotificationRepository}와 빈 이름 충돌을 피하기 위해 별도 명명.
 */
public interface CommunityNotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);
}
