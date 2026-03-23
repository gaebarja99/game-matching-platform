package com.gamematcher.repository;

import com.gamematcher.entity.GroupChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupChatRoomMemberRepository extends JpaRepository<GroupChatRoomMember, Long> {

    List<GroupChatRoomMember> findByUserIdOrderByJoinedAtDesc(Long userId);

    List<GroupChatRoomMember> findByRoomId(Long roomId);

    Optional<GroupChatRoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
