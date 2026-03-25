package com.gamematcher.repository;

import com.gamematcher.entity.StreamBlacklist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StreamBlacklistRepository extends JpaRepository<StreamBlacklist, Long> {

    boolean existsByStreamIdAndUserId(Long streamId, Long userId);

    List<StreamBlacklist> findByStreamIdOrderByIdDesc(Long streamId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM StreamBlacklist b WHERE b.streamId = :streamId AND b.userId = :userId")
    void deleteByStreamIdAndUserId(@Param("streamId") Long streamId, @Param("userId") Long userId);
}
