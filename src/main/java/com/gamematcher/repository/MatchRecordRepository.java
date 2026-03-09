package com.gamematcher.repository;

import com.gamematcher.entity.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

    Optional<MatchRecord> findByMatchId(String matchId);

    List<MatchRecord> findByGame_IdOrderByPlayedAtDesc(Long gameId, org.springframework.data.domain.Pageable pageable);
}
