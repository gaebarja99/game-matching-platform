package com.gamematcher.repository.ai.evaluation;

import com.gamematcher.entity.ai.evaluation.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

    Optional<MatchRecord> findByMatchId(String matchId);

    Optional<MatchRecord> findByGame_CodeAndMatchId(String code, String matchId);

    List<MatchRecord> findByGame_IdOrderByPlayedAtDesc(Long gameId, org.springframework.data.domain.Pageable pageable);
}
