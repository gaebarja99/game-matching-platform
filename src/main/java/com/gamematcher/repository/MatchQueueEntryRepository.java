package com.gamematcher.repository;

import com.gamematcher.entity.MatchQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchQueueEntryRepository extends JpaRepository<MatchQueueEntry, Long> {

    List<MatchQueueEntry> findByGameOrderByJoinedAtAsc(String game);

    List<MatchQueueEntry> findByGameAndMaxPlayersOrderByJoinedAtAsc(String game, Integer maxPlayers);

    Optional<MatchQueueEntry> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);

    long countByGame(String game);

    long countByGameAndMaxPlayers(String game, Integer maxPlayers);
}
