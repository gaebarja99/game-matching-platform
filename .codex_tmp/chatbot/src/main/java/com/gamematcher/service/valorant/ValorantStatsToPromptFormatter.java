package com.gamematcher.service.valorant;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.ValorantMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ValorantPlayerMatchStatsDTO를 LLM 프롬프트에 사용할 텍스트로 변환.
 * 매치/라운드 스탯을 사람/모델이 읽기 쉬운 형식으로 포맷한다.
 */
@Component
public class ValorantStatsToPromptFormatter {

    /** 라운드별 예시에 포함할 최대 라운드 수 (기본: 2) */
    private static final int DEFAULT_MAX_ROUND_EXAMPLES = 2;

    /**
     * 단일 플레이어 스탯을 프롬프트용 텍스트로 변환.
     *
     * @param playerStats 플레이어 매치 스탯 (null 허용)
     * @return 포맷된 텍스트 (null이면 빈 문자열)
     */
    public String format(ValorantPlayerMatchStatsDTO playerStats) {
        return format(playerStats, DEFAULT_MAX_ROUND_EXAMPLES);
    }

    /**
     * 단일 플레이어 스탯을 프롬프트용 텍스트로 변환.
     *
     * @param playerStats      플레이어 매치 스탯 (null 허용)
     * @param maxRoundExamples 라운드별 예시에 포함할 최대 라운드 수 (0 이하면 전체 제외)
     * @return 포맷된 텍스트
     */
    public String format(ValorantPlayerMatchStatsDTO playerStats, int maxRoundExamples) {
        if (playerStats == null) return "";
        StringBuilder sb = new StringBuilder();
        appendPlayerStatsSummary(sb, playerStats, maxRoundExamples);
        return sb.toString();
    }

    /**
     * 전체 플레이어 스탯을 프롬프트용 텍스트로 변환.
     *
     * @param playerStats 플레이어별 매치 스탯 목록
     * @return 모든 플레이어 스탯이 포함된 텍스트
     */
    public String formatAll(List<ValorantPlayerMatchStatsDTO> playerStats) {
        return formatAll(playerStats, DEFAULT_MAX_ROUND_EXAMPLES);
    }

    /**
     * 전체 플레이어 스탯을 프롬프트용 텍스트로 변환.
     *
     * @param playerStats      플레이어별 매치 스탯 목록
     * @param maxRoundExamples 라운드별 예시에 포함할 최대 라운드 수
     * @return 모든 플레이어 스탯이 포함된 텍스트
     */
    public String formatAll(List<ValorantPlayerMatchStatsDTO> playerStats, int maxRoundExamples) {
        if (playerStats == null || playerStats.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < playerStats.size(); i++) {
            if (i > 0) sb.append("\n");
            appendPlayerStatsSummary(sb, playerStats.get(i), maxRoundExamples);
        }
        return sb.toString();
    }

    /**
     * LLM용 압축 요약 텍스트 생성. 토큰 비용 절감을 위해 매치/라운드 스탯을 간결하게 포맷.
     *
     * @param playerStats 플레이어 매치 스탯 (null 허용)
     * @return [매치 요약] + [라운드별] 블록 (null이면 빈 문자열)
     */
    public String formatSummary(ValorantPlayerMatchStatsDTO playerStats) {
        return formatSummary(playerStats, -1);
    }

    /**
     * LLM용 압축 요약 텍스트 생성.
     *
     * @param playerStats   플레이어 매치 스탯
     * @param maxRoundLines 라운드별에 포함할 최대 라운드 수 (0=제외, -1=전체)
     * @return 요약 텍스트
     */
    public String formatSummary(ValorantPlayerMatchStatsDTO playerStats, int maxRoundLines) {
        if (playerStats == null) return "";
        StringBuilder sb = new StringBuilder();

        appendMatchSummaryBlock(sb, playerStats);
        if (playerStats.getRoundStats() != null && !playerStats.getRoundStats().isEmpty() && maxRoundLines != 0) {
            appendRoundLinesBlock(sb, playerStats.getRoundStats(), maxRoundLines);
        }

        return sb.toString();
    }

    private void appendMatchSummaryBlock(StringBuilder sb, ValorantPlayerMatchStatsDTO ps) {
        String displayName = ps.getPlayerDisplayName() != null ? ps.getPlayerDisplayName() : "(이름없음)";
        String team = ps.getPlayerTeam() != null ? ps.getPlayerTeam() : "-";
        String agent = ps.getAgent() != null ? ps.getAgent() : "-";

        sb.append("[매치 요약]\n");
        sb.append("플레이어: ").append(displayName).append(" | 팀: ").append(team).append(" | 에이전트: ").append(agent).append("\n");

        if (ps.getMatchStats() == null) {
            sb.append("매치 스탯 없음\n");
            return;
        }

        var ms = ps.getMatchStats();
        String resultKr = toResultKorean(ms.getResult());
        int roundsLost = ms.getRoundsPlayed() - ms.getRoundsWon();
        sb.append("결과: ").append(resultKr).append(" | 스코어: ").append(ms.getRoundsWon()).append("-")
                .append(roundsLost).append(" (").append(ms.getRoundsPlayed()).append("라운드)\n");
        sb.append("승리기여도 점수: ").append(ms.getMatchAverageContributionScore()).append("\n\n");

        sb.append("KDA: ").append(ms.getKills()).append("/").append(ms.getDeaths()).append("/").append(ms.getAssists())
                .append(" (KD ").append(String.format("%.2f", ms.getKd())).append(")\n");
        sb.append("KAST: ").append(Math.round(ms.getKast())).append("% | ADR: ").append(Math.round(ms.getAdr()))
                .append(" | 평균피해격차: ").append(Math.round(ms.getAvgDamageDifference())).append("\n");
        sb.append("헤드샷율: ").append(Math.round(ms.getHeadShotRate())).append("% | first kill: ")
                .append(ms.getFirstBloods()).append("회 | First Death: ").append(ms.getFirstDeaths()).append("회\n");

        String multiDesc = buildMultiKillDescription(ms);
        sb.append("멀티킬: ").append(ms.getMultiKill()).append(multiDesc).append("\n");
    }

    private String toResultKorean(MatchResult result) {
        if (result == null) return "-";
        return switch (result) {
            case VICTORY -> "승리";
            case DEFEAT -> "패배";
            case DRAW -> "무승부";
        };
    }

    private String buildMultiKillDescription(ValorantMatchStatsDTO ms) {
        if (ms.getMultiKill() == 0) return "";
        StringBuilder sb = new StringBuilder(" (");
        boolean first = true;
        if (ms.getDoubleKill() > 0) {
            sb.append("더블킬 ").append(ms.getDoubleKill()).append("회");
            first = false;
        }
        if (ms.getTripleKill() > 0) {
            if (!first) sb.append(", ");
            sb.append("트리플킬 ").append(ms.getTripleKill()).append("회");
            first = false;
        }
        if (ms.getQuadraKill() > 0) {
            if (!first) sb.append(", ");
            sb.append("쿼드라킬 ").append(ms.getQuadraKill()).append("회");
            first = false;
        }
        if (ms.getPentaKill() > 0) {
            if (!first) sb.append(", ");
            sb.append("펜타킬 ").append(ms.getPentaKill()).append("회");
            first = false;
        }
        if (ms.getOverKill() > 0) {
            if (!first) sb.append(", ");
            sb.append("오버킬 ").append(ms.getOverKill()).append("회");
        }
        sb.append(")");
        return sb.toString();
    }

    private void appendRoundLinesBlock(StringBuilder sb, List<ValorantRoundStatsDTO> roundStats, int maxLines) {
        sb.append("\n[라운드별] (FK=첫킬 FD=첫데스 T=트레이드)\n");
        int count = maxLines < 0 ? roundStats.size() : Math.min(maxLines, roundStats.size());
        for (int i = 0; i < count; i++) {
            var rs = roundStats.get(i);
            String winLose = rs.isRoundWon() ? "승리" : "패배";
            int died = rs.isDied() ? 1 : 0;
            sb.append("R").append(rs.getRoundIndex()).append(": ").append(winLose)
                    .append(" 킬").append(rs.getKills()).append(" 데스").append(died)
                    .append(" 딜").append(rs.getDamage()).append(" 승리기여도").append(rs.getRoundContributionScore());
            if (rs.isFirstKill()) sb.append(" FK");
            if (rs.isFirstDeath()) sb.append(" FD");
            if (rs.isTraded()) sb.append(" T");
            sb.append("\n");
        }
    }

    private void appendPlayerStatsSummary(StringBuilder sb, ValorantPlayerMatchStatsDTO ps, int maxRoundExamples) {
        if (ps == null) return;
        String displayName = ps.getPlayerDisplayName() != null ? ps.getPlayerDisplayName() : "(이름없음)";
        String puuid = ps.getPlayerPuuid() != null ? ps.getPlayerPuuid() : "(puuid없음)";
        sb.append("--- 플레이어: ").append(displayName).append(" (").append(puuid).append(")\n");
        if (ps.getMatchId() != null) {
            sb.append("  매치ID: ").append(ps.getMatchId()).append("\n");
        }
        sb.append("  팀: ").append(ps.getPlayerTeam() != null ? ps.getPlayerTeam() : "-")
                .append(", 에이전트: ").append(ps.getAgent() != null ? ps.getAgent() : "-").append("\n");
        if (ps.getMatchStats() != null) {
            var ms = ps.getMatchStats();
            sb.append("  [매치 스탯] 결과: ").append(ms.getResult()).append(", K/D/A: ")
                    .append(ms.getKills()).append("/").append(ms.getDeaths()).append("/").append(ms.getAssists());
            sb.append(", Rounds: ").append(ms.getRoundsWon()).append("/").append(ms.getRoundsPlayed());
            sb.append(", ADR: ").append(String.format("%.1f", ms.getAdr()));
            sb.append(", KAST: ").append(String.format("%.1f%%", ms.getKast()));
            sb.append(", 헤드샷률: ").append(String.format("%.1f%%", ms.getHeadShotRate())).append("\n");
            sb.append("  [엔트리] FirstBlood: ").append(ms.getFirstBloods()).append(", FirstDeath: ").append(ms.getFirstDeaths());
            sb.append(", 멀티킬: ").append(ms.getMultiKill())
                    .append(" (2K:").append(ms.getDoubleKill())
                    .append("/3K:").append(ms.getTripleKill())
                    .append("/4K:").append(ms.getQuadraKill())
                    .append("/5K:").append(ms.getPentaKill())
                    .append("/6K+:").append(ms.getOverKill()).append(")");
            sb.append(", DDΔ: ").append(String.format("%.1f", ms.getAvgDamageDifference())).append("\n");
            sb.append("  [매치 평균 기여도 점수]: ").append(ms.getMatchAverageContributionScore())
                    .append(" (라운드 기여도 평균, 100기준)\n");
        } else {
            sb.append("  [매치 스탯] (없음)\n");
        }
        sb.append("  [라운드 수]: ").append(ps.getRoundStats() != null ? ps.getRoundStats().size() : 0).append("\n");
        if (ps.getRoundStats() != null && !ps.getRoundStats().isEmpty() && maxRoundExamples > 0) {
            appendRoundStatsSummary(sb, ps.getRoundStats(), maxRoundExamples);
        }
    }

    private void appendRoundStatsSummary(StringBuilder sb, List<ValorantRoundStatsDTO> roundStats, int maxExamples) {
        int count = Math.min(maxExamples, roundStats.size());
        String header = count == 2 ? "1·2라운드" : count + "라운드";
        sb.append("  [라운드별 예시 - ").append(header).append("]\n");
        for (int i = 0; i < count; i++) {
            var rs = roundStats.get(i);
            sb.append("    R").append(rs.getRoundIndex()).append(": damage=").append(rs.getDamage());
            sb.append(", kills=").append(rs.getKills());
            sb.append(", roundWon=").append(rs.isRoundWon());
            sb.append(", died=").append(rs.isDied());
            sb.append(", FK=").append(rs.isFirstKill());
            sb.append(", FD=").append(rs.isFirstDeath());
            sb.append(", roundContributionScore=").append(rs.getRoundContributionScore());
            sb.append("\n");
        }
        double avgScore = roundStats.stream()
                .mapToInt(ValorantRoundStatsDTO::getRoundContributionScore)
                .average()
                .orElse(0);
        sb.append("  [라운드 기여도 평균]: ").append(String.format("%.1f", avgScore)).append("\n");
    }
}
