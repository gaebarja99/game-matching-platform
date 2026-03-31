package com.gamematcher.repository;

import com.gamematcher.entity.LiveStreamChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LiveStreamChatMessageRepository extends JpaRepository<LiveStreamChatMessage, Long> {

    /** 최근 채팅 내역 (최신순으로 limit개 조회 후, 서비스에서 과거순으로 뒤집어 표시) */
    List<LiveStreamChatMessage> findByStreamIdOrderByCreatedAtDesc(Long streamId, Pageable pageable);

    long countByStreamId(Long streamId);

    @Query("SELECT COUNT(DISTINCT m.userId) FROM LiveStreamChatMessage m WHERE m.streamId = :streamId")
    long countDistinctUserIdByStreamId(@Param("streamId") Long streamId);
}
