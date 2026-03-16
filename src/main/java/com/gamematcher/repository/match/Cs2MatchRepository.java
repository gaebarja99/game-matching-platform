package com.gamematcher.repository.match;

import com.gamematcher.entity.match.cs2.Cs2Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Cs2MatchRepository extends JpaRepository<Cs2Match, Long> {
    boolean existsBySteamIdAndMatchId(String steamId, String matchId);
}
