package com.gamematcher.repository.match;

import com.gamematcher.entity.match.pubg.PubgMatchAiEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PubgMatchAiEvaluationRepository extends JpaRepository<PubgMatchAiEvaluation, Long> {

    Optional<PubgMatchAiEvaluation> findByPubgMatchParticipantId(Long pubgMatchParticipantId);

    List<PubgMatchAiEvaluation> findByPubgMatchParticipant_Match_MatchId(String matchId);

    boolean existsByPubgMatchParticipantId(Long pubgMatchParticipantId);
}

