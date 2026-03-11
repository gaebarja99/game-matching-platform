package com.gamematcher.repository.match;

import com.gamematcher.entity.match.lol.LolMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LolMatchRepository extends JpaRepository<LolMatch, Long> {

    boolean existsByMatchId(String matchId);

    Optional<LolMatch> findByMatchId(String matchId);
}
