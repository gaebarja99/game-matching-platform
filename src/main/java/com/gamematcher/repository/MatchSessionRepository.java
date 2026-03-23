package com.gamematcher.repository;

import com.gamematcher.entity.MatchSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchSessionRepository extends JpaRepository<MatchSession, Long> {

    List<MatchSession> findAllByOrderByCreatedAtDesc();
}
