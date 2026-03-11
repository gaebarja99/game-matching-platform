package com.gamematcher.repository.match;

import com.gamematcher.entity.match.MatchSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSummaryRepository extends JpaRepository<MatchSummary, Long> {

    boolean existsByPuuidAndMatchId(String puuid, String matchId);
}
