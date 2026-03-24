package com.gamematcher.repository;

import com.gamematcher.entity.LiveStreamChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LiveStreamChatMessageRepository extends JpaRepository<LiveStreamChatMessage, Long> {

    List<LiveStreamChatMessage> findByStreamIdOrderByCreatedAtDesc(Long streamId, Pageable pageable);

    List<LiveStreamChatMessage> findByStreamIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long streamId,
            LocalDateTime createdAt,
            Pageable pageable
    );
}
