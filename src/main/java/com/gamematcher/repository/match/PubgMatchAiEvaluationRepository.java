package com.gamematcher.repository.match;

import com.gamematcher.entity.match.pubg.PubgMatchAiEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PubgMatchAiEvaluationRepository extends JpaRepository<PubgMatchAiEvaluation, Long> {

    Optional<PubgMatchAiEvaluation> findByPubgMatchParticipantId(Long pubgMatchParticipantId);

    List<PubgMatchAiEvaluation> findByPubgMatchParticipant_Match_MatchId(String matchId);

    @Query(
            "select e from PubgMatchAiEvaluation e "
                    + "join fetch e.pubgMatchParticipant p "
                    + "join fetch p.match m where m.matchId = :matchId")
    List<PubgMatchAiEvaluation> findByMatchIdWithParticipantFetched(@Param("matchId") String matchId);

    boolean existsByPubgMatchParticipantId(Long pubgMatchParticipantId);
}

