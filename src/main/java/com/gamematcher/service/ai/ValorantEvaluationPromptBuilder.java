package com.gamematcher.service.ai;

import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.service.valorant.ValorantStatsToPromptFormatter;
import org.springframework.stereotype.Component;

/**
 * Valorant 전적 평가를 위한 LLM 프롬프트 구성.
 * 역할·작업·출력형식을 정하고, 게임 데이터를 포함한 전체 프롬프트를 생성한다.
 */
@Component
public class ValorantEvaluationPromptBuilder {

    private static final String ROLE_AND_TASK = """
            [Role] 당신은 발로란트 전문 데이터 분석가이자 최상위권 코치입니다.
            [Task] 제공된 플레이어의 매치 스탯을 분석하여, 데이터 기반의 객관적인 평가와 실전 솔루션을 제공하세요.
            """;

    private static final String ANALYSIS_GUIDELINE = """
            
            [분석 가이드라인]
            1. 지표 해석 우선순위:
               - 타격대/하이브리드: FK(첫 킬) 비율, ADR(150↑ 권장)
               - 전략가/감시자: KAST(70%↑ 권장), FD(첫 데스) 최소화
            2. 데이터 복합 분석: ADR·KD·KAST·HS% 조합을 함께 보고 원인을 추론하세요. (예: ADR 높은데 KD 낮음 → 팀원과 트레이드 부족)
            3. 금기 사항:
               - "에임을 키우세요", "열심히 하세요" 같은 추상적 조언 금지
               - "다음 판에는 [구체적 행동]을 하세요" 형식으로 제언할 것
               - 데이터에 없는 내용을 추측해서 작성하지 말 것
            
            [작성 제약 사항]
            1. 수치 직독 금지: "KDA가 0.47입니다", "FD가 4회입니다"처럼 수치를 그대로 읊는 문장을 절대 사용하지 마십시오. 수치가 의미하는 '현상'을 서술하십시오.
               - 나쁜 예: "FD가 4회로 많습니다."
               - 좋은 예: "반복적인 퍼스트 데스로 팀을 지속적인 수적 열세에 빠뜨리고 있습니다."
            2. 특정 라운드 언급 주의: 맵과 위치 데이터를 모르는 상태에서 "R9처럼 하세요" 같은 맥락 없는 조언을 하지 마십시오. 전체적인 게임 흐름과 교전 성향에만 집중하십시오.
            """;

    private static final String OUTPUT_FORMAT = """
            
            [Output Format]
            - 반드시 한국어로 작성하십시오.
            - 응답은 반드시 아래 JSON 구조를 지켜야 하며, 다른 텍스트는 포함하지 마십시오.
            - detailedComment 내부에 줄바꿈이 필요할 경우 '\\n' 문자로 표현하여 한 줄 문자열로 만드십시오.
            
            {"summary":"한 줄 요약 (데이터 근거, 100자 이내)","detailedComment":"[강점]... [약점]... [개선안]... (500자 이내, 줄바꿈은 \\n 사용)"}
            """;

    private final ValorantStatsToPromptFormatter statsFormatter;

    public ValorantEvaluationPromptBuilder(ValorantStatsToPromptFormatter statsFormatter) {
        this.statsFormatter = statsFormatter;
    }

    /**
     * LLM에 전달할 전체 프롬프트 생성.
     *
     * @param playerStats 플레이어 매치 스탯
     * @return 역할 + 데이터 + 출력형식이 포함된 프롬프트
     */
    public String build(ValorantPlayerMatchStatsDTO playerStats) {
        return build(playerStats, -1);
    }

    /**
     * LLM에 전달할 전체 프롬프트 생성.
     *
     * @param playerStats   플레이어 매치 스탯
     * @param maxRoundLines 라운드별 포함 개수 (0=제외, -1=전체)
     */
    public String build(ValorantPlayerMatchStatsDTO playerStats, int maxRoundLines) {
        if (playerStats == null) return "";
        String statsSummary = statsFormatter.formatSummary(playerStats, maxRoundLines);
        return ROLE_AND_TASK + ANALYSIS_GUIDELINE + "\n" + statsSummary + OUTPUT_FORMAT;
    }
}
