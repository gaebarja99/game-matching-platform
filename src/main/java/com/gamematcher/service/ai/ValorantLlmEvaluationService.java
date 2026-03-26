package com.gamematcher.service.ai;

import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 발로란트 플레이어 전적을 LLM에 전달해 평가(요약·상세 코멘트)를 받는 서비스.
 * {@link ValorantEvaluationPromptBuilder}와 {@link LlmEvaluationService}를 연동한다.
 */
@Service
public class ValorantLlmEvaluationService {

    private final ValorantEvaluationPromptBuilder promptBuilder;
    private final LlmEvaluationService llmService;

    public ValorantLlmEvaluationService(
            ValorantEvaluationPromptBuilder promptBuilder,
            LlmEvaluationService llmService) {
        this.promptBuilder = promptBuilder;
        this.llmService = llmService;
    }

    /**
     * 플레이어 매치 스탯을 분석하여 LLM 평가를 요청한다.
     *
     * @param playerStats 플레이어 매치 스탯 (매치 요약 + 라운드별)
     * @return summary, detailedComment. API 키 없음·호출 실패 시 {@link Optional#empty()}
     */
    public Optional<LlmEvaluationResponseDTO> evaluate(ValorantPlayerMatchStatsDTO playerStats) {
        return evaluate(playerStats, -1, null);
    }

    /**
     * 플레이어 매치 스탯을 분석하여 LLM 평가를 요청한다.
     *
     * @param playerStats   플레이어 매치 스탯
     * @param maxRoundLines 라운드별 포함 개수 (0=제외, -1=전체)
     * @return summary, detailedComment. API 키 없음·호출 실패 시 {@link Optional#empty()}
     */
    public Optional<LlmEvaluationResponseDTO> evaluate(
            ValorantPlayerMatchStatsDTO playerStats,
            int maxRoundLines) {
        return evaluate(playerStats, maxRoundLines, null);
    }

    /**
     * @param modelOverride 비어 있지 않으면 해당 OpenAI 모델로 호출
     */
    public Optional<LlmEvaluationResponseDTO> evaluate(
            ValorantPlayerMatchStatsDTO playerStats,
            int maxRoundLines,
            String modelOverride) {
        if (playerStats == null) {
            return Optional.empty();
        }
        String prompt = promptBuilder.build(playerStats, maxRoundLines);
        return llmService.evaluate(prompt, modelOverride);
    }
}
