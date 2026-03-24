package com.gamematcher.repository;

import com.gamematcher.entity.StreamManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StreamManagerRepository extends JpaRepository<StreamManager, Long> {

    boolean existsByStreamIdAndUserId(Long streamId, Long userId);

    List<StreamManager> findByStreamIdOrderByIdDesc(Long streamId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM StreamManager m WHERE m.streamId = :streamId AND m.userId = :userId")
    void deleteByStreamIdAndUserId(@Param("streamId") Long streamId, @Param("userId") Long userId);
}
