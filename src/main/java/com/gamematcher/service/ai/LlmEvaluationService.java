package com.gamematcher.service.ai;

import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;

import java.util.Optional;

/**
 * LLM API를 호출하여 전적 평가(요약·상세 코멘트)를 생성하는 서비스.
 */
public interface LlmEvaluationService {

    /**
     * 프롬프트를 LLM에 전달하여 평가 결과를 받는다.
     *
     * @param promptText LLM에 전달할 프롬프트 전문
     * @return summary, detailedComment. API 키 없음·호출 실패 시 {@link Optional#empty()}
     */
    default Optional<LlmEvaluationResponseDTO> evaluate(String promptText) {
        return evaluate(promptText, null);
    }

    /**
     * @param modelOverride 비어 있지 않으면 이 모델 ID로 호출 (예: gpt-4o-mini). null/공백이면 설정 기본 모델.
     */
    Optional<LlmEvaluationResponseDTO> evaluate(String promptText, String modelOverride);
}
