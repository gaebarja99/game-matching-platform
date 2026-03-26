package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ValorantMatchDetailRepository extends JpaRepository<ValorantMatch, Long> {

    boolean existsByMatchId(String matchId);

    Optional<ValorantMatch> findByMatchId(String matchId);

    @Query("SELECT m FROM ValorantMatch m LEFT JOIN FETCH m.players WHERE m.matchId = :matchId")
    Optional<ValorantMatch> findByMatchIdWithPlayers(@Param("matchId") String matchId);
}
