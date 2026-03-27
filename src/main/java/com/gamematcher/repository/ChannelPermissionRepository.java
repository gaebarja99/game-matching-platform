package com.gamematcher.repository;

import com.gamematcher.entity.ChannelPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelPermissionRepository extends JpaRepository<ChannelPermission, Long> {

    List<ChannelPermission> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<ChannelPermission> findByManagerUserIdOrderByCreatedAtDesc(Long managerUserId);

    Optional<ChannelPermission> findByOwnerUserIdAndManagerUserId(Long ownerUserId, Long managerUserId);

    boolean existsByOwnerUserIdAndManagerUserId(Long ownerUserId, Long managerUserId);

    void deleteByOwnerUserIdAndManagerUserId(Long ownerUserId, Long managerUserId);
}
