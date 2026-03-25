package com.gamematcher.repository;

import com.gamematcher.entity.GroupChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage, Long> {

    List<GroupChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    List<GroupChatMessage> findByRoomId(Long roomId);
}
