package com.gamematcher.repository.common;

import com.gamematcher.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    List<BlockedUser> findByBlockerId(Long blockerId);
    boolean existsByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);
    Optional<BlockedUser> findByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);
}
