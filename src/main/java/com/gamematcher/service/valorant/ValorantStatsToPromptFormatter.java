package com.gamematcher.service.valorant;

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
