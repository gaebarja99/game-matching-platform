package com.gamematcher.repository.match;

import com.gamematcher.entity.match.TftMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TftMatchRepository extends JpaRepository<TftMatch, Long> {

    boolean existsByPuuidAndMatchId(String puuid, String matchId);
}
