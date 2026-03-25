package com.gamematcher.repository;

import com.gamematcher.entity.MatchSessionMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchSessionMemberRepository extends JpaRepository<MatchSessionMember, Long> {

    List<MatchSessionMember> findBySessionId(Long sessionId);

    List<MatchSessionMember> findByUserIdOrderByJoinedAtDesc(Long userId);

    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);

    Optional<MatchSessionMember> findBySessionIdAndUserId(Long sessionId, Long userId);

    void deleteBySessionId(Long sessionId);
}
