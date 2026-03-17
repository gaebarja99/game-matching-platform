package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMatchAiEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValorantMatchAiEvaluationRepository extends JpaRepository<ValorantMatchAiEvaluation, Long> {

    Optional<ValorantMatchAiEvaluation> findByValorantMatchPlayerId(Long valorantMatchPlayerId);

    List<ValorantMatchAiEvaluation> findByValorantMatchPlayer_Match_MatchId(String matchId);

    boolean existsByValorantMatchPlayerId(Long valorantMatchPlayerId);
}
