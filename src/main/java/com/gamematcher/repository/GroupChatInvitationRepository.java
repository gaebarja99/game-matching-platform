package com.gamematcher.repository;

import com.gamematcher.entity.GroupChatInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupChatInvitationRepository extends JpaRepository<GroupChatInvitation, Long> {

    List<GroupChatInvitation> findByToUserIdAndStatusOrderByCreatedAtDesc(Long toUserId, GroupChatInvitation.InvitationStatus status);

    Optional<GroupChatInvitation> findByRoomIdAndToUserId(Long roomId, Long toUserId);

    boolean existsByRoomIdAndToUserIdAndStatus(Long roomId, Long toUserId, GroupChatInvitation.InvitationStatus status);

    List<GroupChatInvitation> findByRoomId(Long roomId);
}
