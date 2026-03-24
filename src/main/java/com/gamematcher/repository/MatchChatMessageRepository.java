package com.gamematcher.repository;

import com.gamematcher.entity.MatchChatMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchChatMessageRepository extends JpaRepository<MatchChatMessage, Long> {

    List<MatchChatMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId, PageRequest pageRequest);

    void deleteBySessionId(Long sessionId);
}
