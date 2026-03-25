package com.gamematcher.repository;

import com.gamematcher.entity.GameRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRoomMemberRepository extends JpaRepository<GameRoomMember, Long> {

    List<GameRoomMember> findByRoomId(Long roomId);

    List<GameRoomMember> findByUserIdOrderByJoinedAtDesc(Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    Optional<GameRoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomId(Long roomId);
}
