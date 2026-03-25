package com.gamematcher.repository.match;

import com.gamematcher.entity.match.pubg.PubgMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PubgMatchRepository extends JpaRepository<PubgMatch, Long> {
    Optional<PubgMatch> findByMatchId(String matchId);

    boolean existsByMatchId(String matchId);
}

