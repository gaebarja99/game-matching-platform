package com.gamematcher.service.pubg;

import com.gamematcher.dto.ai.evaluation.PubgPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.PubgTimelineEventLineDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PUBG 플레이어 매치 스탯(프롬프트 입력용 DTO)을 사람이 읽기 쉬운 텍스트로 변환.
 */
@Component
public class PubgStatsToPromptFormatter {

    public String formatSummary(PubgPlayerMatchStatsDTO stats, int maxTimelineLines) {
        if (stats == null) return "";

        StringBuilder sb = new StringBuilder();

        appendMatchSummaryBlock(sb, stats);
        List<PubgTimelineEventLineDTO> lines = stats.getTimelineLines();
        if (lines != null && !lines.isEmpty() && maxTimelineLines != 0) {
            appendTimelineBlock(sb, lines, maxTimelineLines);
        }
        return sb.toString();
    }

    private void appendMatchSummaryBlock(StringBuilder sb, PubgPlayerMatchStatsDTO stats) {
        sb.append("[매치 요약]\n");

        String playerName = stats.getPlayerName() != null ? stats.getPlayerName() : "(이름없음)";
        sb.append("플레이어: ").append(playerName);
        if (stats.getAccountId() != null && !stats.getAccountId().isBlank()) {
            sb.append(" (").append(shortenAccount(stats.getAccountId())).append(")");
        }
        sb.append(" | 팀: ").append(stats.getTeamId() != null ? stats.getTeamId() : "-");
        sb.append(" | 맵: ").append(stats.getMapName() != null ? stats.getMapName() : "-").append("\n");

        sb.append("결과: ").append(stats.isWon() ? "승리" : "패배");
        if (stats.getWinPlace() != null) {
            sb.append(" | 최종 순위: ").append(stats.getWinPlace());
        }
        if (stats.getKills() != null || stats.getAssists() != null) {
            Integer kills = stats.getKills();
            Integer assists = stats.getAssists();
            sb.append(" | 킬/어시스트: ");
            sb.append(kills != null ? kills : 0);
            sb.append("/");
            sb.append(assists != null ? assists : 0);
        }
        sb.append("\n");
    }

    private void appendTimelineBlock(StringBuilder sb, List<PubgTimelineEventLineDTO> lines, int maxLines) {
        sb.append("\n[주요 타임라인]\n");
        int count = maxLines < 0 ? lines.size() : Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            PubgTimelineEventLineDTO line = lines.get(i);
            if (line == null) continue;
            sb.append(formatElapsed(line.getElapsedSeconds()))
                    .append(" ")
                    .append(line.getLabel() != null ? line.getLabel() : "-")
                    .append(" ")
                    .append(line.getMessage() != null ? line.getMessage() : "-")
                    .append("\n");
        }
    }

    private String shortenAccount(String accountId) {
        if (accountId == null) return null;
        if (accountId.length() <= 10) return accountId;
        return accountId.substring(0, 8) + "...";
    }

    private String formatElapsed(Long elapsedSeconds) {
        long s = elapsedSeconds != null ? elapsedSeconds : 0;
        long m = s / 60;
        long sec = s % 60;
        return m + ":" + String.format("%02d", sec);
    }
}

