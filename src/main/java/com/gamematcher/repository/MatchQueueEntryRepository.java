package com.gamematcher.repository;

import com.gamematcher.entity.MatchQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchQueueEntryRepository extends JpaRepository<MatchQueueEntry, Long> {

    long countByGame(String game);

    List<MatchQueueEntry> findByGameOrderByJoinedAtAsc(String game);

    Optional<MatchQueueEntry> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}
