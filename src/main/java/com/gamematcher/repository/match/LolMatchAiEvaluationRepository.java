package com.gamematcher.repository.match;

import com.gamematcher.entity.match.lol.LolMatchAiEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LolMatchAiEvaluationRepository extends JpaRepository<LolMatchAiEvaluation, Long> {

    Optional<LolMatchAiEvaluation> findByLolMatchParticipant_IdAndLlmModel(Long lolMatchParticipantId, String llmModel);
}
