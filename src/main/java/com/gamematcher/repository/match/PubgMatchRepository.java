package com.gamematcher.repository.match;

import com.gamematcher.entity.match.pubg.PubgMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PubgMatchRepository extends JpaRepository<PubgMatch, Long> {

    boolean existsByMatchId(String matchId);

    Optional<PubgMatch> findByMatchId(String matchId);

    @Query("SELECT m FROM PubgMatch m LEFT JOIN FETCH m.participants WHERE m.matchId = :matchId")
    Optional<PubgMatch> findByMatchIdWithParticipants(@Param("matchId") String matchId);
}

