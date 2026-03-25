package com.gamematcher.repository;

import com.gamematcher.entity.StreamChatBan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StreamChatBanRepository extends JpaRepository<StreamChatBan, Long> {

    boolean existsByStreamIdAndUserId(Long streamId, Long userId);

    Optional<StreamChatBan> findByStreamIdAndUserId(Long streamId, Long userId);

    List<StreamChatBan> findByStreamIdOrderByIdDesc(Long streamId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM StreamChatBan b WHERE b.streamId = :streamId AND b.userId = :userId")
    void deleteByStreamIdAndUserId(@Param("streamId") Long streamId, @Param("userId") Long userId);
}
