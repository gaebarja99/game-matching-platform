package com.gamematcher.repository.match;

import com.gamematcher.entity.match.TftMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TftMatchRepository extends JpaRepository<TftMatch, Long> {

    boolean existsByPuuidAndMatchId(String puuid, String matchId);

    Optional<TftMatch> findByPuuidAndMatchId(String puuid, String matchId);
}
