package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValorantMatchDetailRepository extends JpaRepository<ValorantMatch, Long> {

    boolean existsByMatchId(String matchId);

    Optional<ValorantMatch> findByMatchId(String matchId);
}
