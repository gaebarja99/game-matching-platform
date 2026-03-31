package com.gamematcher.repository.match;

import com.gamematcher.entity.match.lol.LolMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LolMatchRepository extends JpaRepository<LolMatch, Long> {

    boolean existsByMatchId(String matchId);

    Optional<LolMatch> findByMatchId(String matchId);

    @Query("SELECT m FROM LolMatch m LEFT JOIN FETCH m.participants WHERE m.matchId = :matchId")
    Optional<LolMatch> findByMatchIdWithParticipants(@Param("matchId") String matchId);
}
