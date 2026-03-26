package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMatchAiEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValorantMatchAiEvaluationRepository extends JpaRepository<ValorantMatchAiEvaluation, Long> {

    Optional<ValorantMatchAiEvaluation> findByValorantMatchPlayer_IdAndLlmModel(Long valorantMatchPlayerId, String llmModel);

    List<ValorantMatchAiEvaluation> findByValorantMatchPlayer_Match_MatchId(String matchId);

    boolean existsByValorantMatchPlayer_Id(Long valorantMatchPlayerId);
}
