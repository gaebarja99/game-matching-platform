package com.gamematcher.service.ai;

import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.LolPlayerMatchStatsDTO;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * LoL 플레이어 전적을 LLM에 전달해 평가(요약·상세 코멘트)를 받는 서비스.
 * {@link LolEvaluationPromptBuilder}와 {@link LlmEvaluationService}를 연동한다.
 */
@Service
public class LolLlmEvaluationService {

    private final LolEvaluationPromptBuilder promptBuilder;
    private final LlmEvaluationService llmService;

    public LolLlmEvaluationService(
            LolEvaluationPromptBuilder promptBuilder,
            LlmEvaluationService llmService) {
        this.promptBuilder = promptBuilder;
        this.llmService = llmService;
    }

    /**
     * 플레이어 매치 스탯을 분석하여 LLM 평가를 요청한다.
     *
     * @param playerStats 플레이어 매치 스탯 (매치 요약 + 라인전·타임라인)
     * @return summary, detailedComment. API 키 없음·호출 실패 시 {@link Optional#empty()}
     */
    public Optional<LlmEvaluationResponseDTO> evaluate(LolPlayerMatchStatsDTO playerStats) {
        return evaluate(playerStats, -1);
    }

    /**
     * 플레이어 매치 스탯을 분석하여 LLM 평가를 요청한다.
     *
     * @param playerStats       플레이어 매치 스탯
     * @param maxTimelineEvents 타임라인 이벤트 포함 개수 (0=제외, -1=전체)
     * @return summary, detailedComment. API 키 없음·호출 실패 시 {@link Optional#empty()}
     */
    public Optional<LlmEvaluationResponseDTO> evaluate(
            LolPlayerMatchStatsDTO playerStats,
            int maxTimelineEvents) {
        return evaluate(playerStats, maxTimelineEvents, null);
    }

    /**
     * @param modelOverride 비어 있지 않으면 해당 OpenAI 모델 ID로 호출
     */
    public Optional<LlmEvaluationResponseDTO> evaluate(
            LolPlayerMatchStatsDTO playerStats,
            int maxTimelineEvents,
            String modelOverride) {
        if (playerStats == null) {
            return Optional.empty();
        }
        String prompt = promptBuilder.build(playerStats, maxTimelineEvents);
        return llmService.evaluate(prompt, modelOverride);
    }
}
