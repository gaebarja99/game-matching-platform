package com.gamematcher.service.ai;

import com.gamematcher.dto.ai.evaluation.LolPlayerMatchStatsDTO;
import com.gamematcher.service.lol.LolStatsToPromptFormatter;
import org.springframework.stereotype.Component;

/**
 * LoL 전적 평가를 위한 LLM 프롬프트 구성.
 * 역할·작업·출력형식을 정하고, 게임 데이터를 포함한 전체 프롬프트를 생성한다.
 */
@Component
public class LolEvaluationPromptBuilder {

    private static final String ROLE_AND_TASK = """
            [Role] 당신은 리그 오브 레전드 전문 데이터 분석가이자 최상위권 코치입니다.
            [Task] 제공된 플레이어의 매치 스탯을 분석하여, 데이터 기반의 객관적인 평가와 실전 솔루션을 제공하세요.
            """;

    private static final String ANALYSIS_GUIDELINE = """
            
            [분석 가이드라인]
            1. 지표 해석 우선순위:
               - 라이너(탑/미드/봇): 라인전 골드·CS·XP 차이, 딜량, 시야점수
               - 정글: 오브젝트 타임라인(드래곤·바론·전령), 킬 참여 패턴
               - 서포터: 시야점수, 어시스트, 포탑 방패/파괴 관련 기여
            2. 데이터 복합 분석: 라인전 지표와 팀 지표를 함께 보고 원인을 추론하세요. (예: 라인 우위인데 팀 열세 → 로밍·오브젝트 타이밍 문제)
            3. 라인전·팀 지표 인과관계: 라인전 지표와 팀 지표의 인과관계를 연결하여 해석하세요. (예: 팀 골드가 급격히 뒤처진 시점에 플레이어의 데스가 상대본진에서 발생했다면, 그 연관성을 짚어주세요.)
            4. 챔피언 특성 반영: 분석 시 해당 챔피언의 특성(예: 기동성, 궁극기 의존도, 아이템 시너지)을 고려하여 코멘트를 작성하세요. 단순히 포지션만이 아니라 '이 챔피언을 하는 플레이어'로서 코칭하세요.
            5. 금기 사항:
               - "CS를 더 챙기세요", "열심히 하세요" 같은 추상적 조언 금지
               - "다음 판에는 [구체적 행동]을 하세요" 형식으로 제언할 것
               - 데이터에 없는 내용을 추측해서 작성하지 말 것
            
            [작성 제약 사항]
            1. 수치 직독 금지: "KDA가 2/4/5입니다", "CS가 -6입니다"처럼 수치를 그대로 읊는 문장을 절대 사용하지 마십시오. 수치가 의미하는 '현상'을 서술하십시오.
               - 나쁜 예: "라인 골드가 -125로 밀렸습니다."
               - 좋은 예: "초반 라인전에서 상대에 비해 골드 격차가 벌어지며 아이템 타이밍이 뒤쳐졌습니다."
            2. 타임라인 활용: 단순히 특정 시간에 발생한 이벤트를 비난하지 말고, 타임라인상의 전후 관계(예: 아이템 완성 직후의 데스, 6레벨 달성 후의 교전)를 연결하여 '결정적 승부처'를 분석하세요.
            """;

    private static final String OUTPUT_FORMAT = """
            
            [Output Format]
            - 반드시 한국어로 작성하십시오.
            - 응답은 반드시 아래 JSON 구조를 지켜야 하며, 다른 텍스트는 포함하지 마십시오.
            - detailedComment 내부에 줄바꿈이 필요할 경우 '\\n' 문자로 표현하여 한 줄 문자열로 만드십시오.
            
            {"summary":"한 줄 요약 (데이터 근거, 100자 이내)","detailedComment":"[강점]... [약점]... [개선안]... (500자 이내, 줄바꿈은 \\n 사용)"}
            """;

    private final LolStatsToPromptFormatter statsFormatter;

    public LolEvaluationPromptBuilder(LolStatsToPromptFormatter statsFormatter) {
        this.statsFormatter = statsFormatter;
    }

    /**
     * LLM에 전달할 전체 프롬프트 생성.
     *
     * @param playerStats 플레이어 매치 스탯
     * @return 역할 + 데이터 + 출력형식이 포함된 프롬프트
     */
    public String build(LolPlayerMatchStatsDTO playerStats) {
        return build(playerStats, -1);
    }

    /**
     * LLM에 전달할 전체 프롬프트 생성.
     *
     * @param playerStats       플레이어 매치 스탯
     * @param maxTimelineEvents 타임라인 이벤트 포함 개수 (0=제외, -1=전체)
     */
    public String build(LolPlayerMatchStatsDTO playerStats, int maxTimelineEvents) {
        if (playerStats == null) return "";
        String statsSummary = statsFormatter.formatSummary(playerStats, maxTimelineEvents);
        return ROLE_AND_TASK + ANALYSIS_GUIDELINE + "\n" + statsSummary + OUTPUT_FORMAT;
    }
}
