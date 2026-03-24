package com.gamematcher.repository;

import com.gamematcher.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    List<FriendRequest> findByToUserIdAndStatusOrderByCreatedAtDesc(Long toUserId, FriendRequest.FriendRequestStatus status);

    List<FriendRequest> findByFromUserIdAndStatus(Long fromUserId, FriendRequest.FriendRequestStatus status);

    boolean existsByFromUserIdAndToUserIdAndStatus(Long fromUserId, Long toUserId, FriendRequest.FriendRequestStatus status);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.status = 'ACCEPTED' AND ((fr.fromUserId = :userId AND fr.toUserId = :friendId) OR (fr.fromUserId = :friendId AND fr.toUserId = :userId))")
    List<FriendRequest> findAcceptedBetween(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
