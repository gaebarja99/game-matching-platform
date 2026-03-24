package com.gamematcher.repository;

import com.gamematcher.entity.DmMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {

    /** 두 사용자 간 대화 (둘 다 발신/수신 포함), 최신순 */
    @Query("SELECT m FROM DmMessage m WHERE (m.fromUserId = :userId AND m.toUserId = :otherUserId) OR (m.fromUserId = :otherUserId AND m.toUserId = :userId) ORDER BY m.createdAt DESC")
    List<DmMessage> findConversation(Long userId, Long otherUserId, Pageable pageable);

    /** 내가 참여한 메시지 최신순 (채팅 목록용) */
    @Query("SELECT m FROM DmMessage m WHERE m.fromUserId = :userId OR m.toUserId = :userId ORDER BY m.createdAt DESC")
    List<DmMessage> findRecentByUserId(Long userId, Pageable pageable);
}
