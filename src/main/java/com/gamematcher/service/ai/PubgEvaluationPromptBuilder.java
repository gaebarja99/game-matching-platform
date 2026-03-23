package com.gamematcher.service.ai;

import com.gamematcher.dto.ai.evaluation.PubgPlayerMatchStatsDTO;
import com.gamematcher.service.pubg.PubgStatsToPromptFormatter;
import org.springframework.stereotype.Component;

/**
 * PUBG 플레이어 LLM 평가 프롬프트 생성.
 */
@Component
public class PubgEvaluationPromptBuilder {

    private static final String ROLE_AND_TASK = """
            [Role] 당신은 배틀그라운드 전문 전술 분석가이자 최상위권 코치입니다.
            [Task] 제공된 플레이어의 텔레메트리 기반 타임라인(샘플링된 의미 라인)을 분석하여,
            교전/이동/아이템 타이밍 관점에서 데이터 근거의 객관적 평가와 실전 솔루션을 제공하세요.
            """;

    private static final String ANALYSIS_GUIDELINE = """

            [분석 가이드라인]
            1. 타임라인 라벨 해석 우선:
               - [운영] 이동/파밍 운영 흐름을 의미
               - [위험] 교전 직전 포지셔닝/접근 구간을 의미
               - [교전] 교전 중 핵심 순간들을 의미
               - [결과] 사망/킬 등 전환 순간을 의미
               - [회복] 회복을 통해 다음 교전으로 이어지는 흐름을 의미
            2. 전후 관계 연결:
               - 운영이 위험/교전으로 전환되는 시점의 원인(위치 선정, 접근 타이밍)을 추론
               - 교전 결과 이후 회복/재정비가 어떻게 이어졌는지 연결
            3. 금기:
               - 데이터에 없는 사실을 추측해 작성하지 말 것
               - "무조건 더 연습하세요" 같은 추상 조언 금지
               - 좌표/원본 좌표 나열(숫자 나열) 금지: 이미 라벨과 지역명으로 바꿔서 제공됨을 전제로 답변할 것
            """;

    private static final String OUTPUT_FORMAT = """

            [Output Format]
            - 반드시 한국어로 작성하십시오.
            - summary와 detailedComment의 모든 문장은 정중한 존댓말(합니다체)로 마무리하십시오.
              예: ~습니다, ~합니다, ~입니다, ~했습니다, ~할 수 있습니다 등. 평서체로 '~다.'로 끝내지 마십시오.
            - 응답은 반드시 아래 JSON 구조를 지켜야 하며, 다른 텍스트는 포함하지 마십시오.
            - detailedComment 내부에 줄바꿈이 필요할 경우 '\\n' 문자로 표현하여 한 줄 문자열로 만드십시오.
            
            {"summary":"한 줄 요약 (데이터 근거, 100자 이내, 합니다체)","detailedComment":"[강점]... [약점]... [개선안]... (500자 이내, 합니다체, 줄바꿈은 \\n 사용)"}
            """;

    private final PubgStatsToPromptFormatter statsFormatter;

    public PubgEvaluationPromptBuilder(PubgStatsToPromptFormatter statsFormatter) {
        this.statsFormatter = statsFormatter;
    }

    public String build(PubgPlayerMatchStatsDTO stats, int maxTimelineLines) {
        if (stats == null) return "";
        String statsSummary = statsFormatter.formatSummary(stats, maxTimelineLines);
        return ROLE_AND_TASK + ANALYSIS_GUIDELINE + "\n" + statsSummary + OUTPUT_FORMAT;
    }
}

