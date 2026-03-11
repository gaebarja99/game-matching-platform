package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValorantMatchRepository extends JpaRepository<ValorantMatch, Long> {

    boolean existsByMatchId(String matchId);
}
