package com.gamematcher.repository.match;

import com.gamematcher.entity.match.lol.LolMatchSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSummaryRepository extends JpaRepository<LolMatchSummary, Long> {

    boolean existsByPuuidAndMatchId(String puuid, String matchId);
}
