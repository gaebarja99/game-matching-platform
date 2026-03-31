package com.gamematcher.service.ai;

import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.PubgPlayerMatchStatsDTO;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * PUBG 플레이어 스탯을 LLM에 전달해 평가(요약·상세 코멘트)를 받는 서비스.
 */
@Service
public class PubgLlmEvaluationService {

    private final PubgEvaluationPromptBuilder promptBuilder;
    private final LlmEvaluationService llmService;

    public PubgLlmEvaluationService(
            PubgEvaluationPromptBuilder promptBuilder,
            LlmEvaluationService llmService
    ) {
        this.promptBuilder = promptBuilder;
        this.llmService = llmService;
    }

    public Optional<LlmEvaluationResponseDTO> evaluate(PubgPlayerMatchStatsDTO stats) {
        return evaluate(stats, -1);
    }

    public Optional<LlmEvaluationResponseDTO> evaluate(PubgPlayerMatchStatsDTO stats, int maxTimelineLines) {
        if (stats == null) return Optional.empty();
        String prompt = promptBuilder.build(stats, maxTimelineLines);
        return llmService.evaluate(prompt);
    }
}

