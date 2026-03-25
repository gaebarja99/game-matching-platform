package com.gamematcher.repository;

import com.gamematcher.entity.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {
    Optional<UserPushToken> findByFcmToken(String fcmToken);
    List<UserPushToken> findByUserId(Long userId);
    void deleteByUserIdAndFcmToken(Long userId, String fcmToken);
}

