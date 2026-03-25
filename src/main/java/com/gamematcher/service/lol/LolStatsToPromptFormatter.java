package com.gamematcher.service.lol;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.LolLaneComparisonDto;
import com.gamematcher.dto.ai.evaluation.LolPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.LolStatsDTO;
import com.gamematcher.dto.ai.evaluation.LolTimelineEventSummaryDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * LolPlayerMatchStatsDTO를 사람이 읽기 쉬운 텍스트로 변환.
 * 눈으로 확인 및 LLM 프롬프트용 포맷팅.
 */
@Component
public class LolStatsToPromptFormatter {

    /**
     * 단일 플레이어 스탯을 텍스트로 변환.
     */
    public String format(LolPlayerMatchStatsDTO playerStats) {
        if (playerStats == null) return "";
        StringBuilder sb = new StringBuilder();
        appendPlayerStats(sb, playerStats);
        return sb.toString();
    }

    /**
     * 전체 플레이어 스탯을 텍스트로 변환.
     */
    public String formatAll(List<LolPlayerMatchStatsDTO> playerStats) {
        if (playerStats == null || playerStats.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < playerStats.size(); i++) {
            if (i > 0) sb.append("\n");
            appendPlayerStats(sb, playerStats.get(i));
        }
        return sb.toString();
    }

    /**
     * LLM용 요약 텍스트 생성.
     */
    public String formatSummary(LolPlayerMatchStatsDTO playerStats) {
        return formatSummary(playerStats, -1);
    }

    /**
     * LLM용 요약 텍스트 생성.
     *
     * @param maxTimelineEvents 타임라인 이벤트 포함 개수 (0=제외, -1=전체)
     */
    public String formatSummary(LolPlayerMatchStatsDTO playerStats, int maxTimelineEvents) {
        if (playerStats == null) return "";
        StringBuilder sb = new StringBuilder();
        appendMatchSummaryBlock(sb, playerStats);
        appendLaneComparisonBlock(sb, playerStats.getLaneComparison());
        if (playerStats.getTimelineEvents() != null && !playerStats.getTimelineEvents().isEmpty() && maxTimelineEvents != 0) {
            appendTimelineEventsBlock(sb, playerStats.getTimelineEvents(), maxTimelineEvents);
        }
        return sb.toString();
    }

    private void appendPlayerStats(StringBuilder sb, LolPlayerMatchStatsDTO ps) {
        if (ps == null) return;
        String displayName = ps.getPlayerDisplayName() != null ? ps.getPlayerDisplayName() : "(이름없음)";
        String puuid = ps.getPlayerPuuid() != null ? ps.getPlayerPuuid() : "(puuid없음)";
        sb.append("--- 플레이어: ").append(displayName).append(" (").append(shortenPuuid(puuid)).append(")\n");
        if (ps.getMatchId() != null) {
            sb.append("  매치ID: ").append(ps.getMatchId()).append("\n");
        }
        sb.append("  챔피언: ").append(ps.getChampion() != null ? ps.getChampion() : "-");
        sb.append(" | 포지션: ").append(positionToKorean(ps.getTeamPosition())).append("\n");
        if (ps.getMatchStats() != null) {
            appendMatchStats(sb, ps.getMatchStats());
        } else {
            sb.append("  [매치 스탯] (없음)\n");
        }
        if (ps.getTimelineEvents() != null && !ps.getTimelineEvents().isEmpty()) {
            sb.append("  [타임라인 이벤트]: ").append(ps.getTimelineEvents().size()).append("건\n");
        }
    }

    private void appendMatchStats(StringBuilder sb, LolStatsDTO ms) {
        String resultKr = toResultKorean(ms.getResult());
        sb.append("  [매치 스탯] 결과: ").append(resultKr);
        sb.append(" | K/D/A: ").append(ms.getKills()).append("/").append(ms.getDeaths()).append("/").append(ms.getAssists());
        sb.append(" | 게임시간: ").append(ms.getGameDurationMinutes()).append("분\n");
        sb.append("  골드: ").append(ms.getGold());
        sb.append(" | CS: ").append(ms.getMinionsKilled());
        sb.append(" | 딜량: ").append(ms.getDamageDealt());
        sb.append(" | 시야점수: ").append(ms.getVisionScore()).append("\n");
    }

    private void appendMatchSummaryBlock(StringBuilder sb, LolPlayerMatchStatsDTO ps) {
        String displayName = ps.getPlayerDisplayName() != null ? ps.getPlayerDisplayName() : "(이름없음)";
        sb.append("[매치 요약]\n");
        sb.append("플레이어: ").append(displayName);
        sb.append(" | 챔피언: ").append(ps.getChampion() != null ? ps.getChampion() : "-");
        sb.append(" | 포지션: ").append(positionToKorean(ps.getTeamPosition())).append("\n");

        if (ps.getMatchStats() == null) {
            sb.append("매치 스탯 없음\n");
            return;
        }
        var ms = ps.getMatchStats();
        String resultKr = toResultKorean(ms.getResult());
        sb.append("결과: ").append(resultKr);
        sb.append(" | K/D/A: ").append(ms.getKills()).append("/").append(ms.getDeaths()).append("/").append(ms.getAssists());
        sb.append(" | 게임시간: ").append(ms.getGameDurationMinutes()).append("분\n");
        sb.append("골드: ").append(ms.getGold()).append(" | CS: ").append(ms.getMinionsKilled());
        sb.append(" | 딜량: ").append(ms.getDamageDealt()).append(" | 시야: ").append(ms.getVisionScore()).append("\n");
    }

    private void appendLaneComparisonBlock(StringBuilder sb, LolLaneComparisonDto lc) {
        if (lc == null) return;
        var snapshots = lc.getSnapshots();
        boolean hasSnapshots = snapshots != null && !snapshots.isEmpty();
        boolean hasChallenges = lc.getMaxCsAdvantageOnLaneOpponent() != null || lc.getLaningPhaseGoldExpAdvantage() != null;
        if (!hasSnapshots && !hasChallenges) return;

        sb.append("\n[라인전·팀 지표 (5분 단위)]\n");
        if (hasSnapshots) {
            for (var s : snapshots) {
                sb.append("- ").append(s.getMinute()).append("분: 라인 골드 ").append(formatDiff(s.getGoldDiffLane()))
                        .append(", CS ").append(formatDiff(s.getCsDiffLane()))
                        .append(", XP ").append(formatDiff(s.getXpDiffLane()))
                        .append(" | 팀 골드 ").append(formatDiff(s.getGoldDiffTeam()))
                        .append(", CS ").append(formatDiff(s.getCsDiffTeam()))
                        .append(", XP ").append(formatDiff(s.getXpDiffTeam()))
                        .append("\n");
            }
        }
        if (hasChallenges) {
            sb.append("(도전과제) 최대 CS 유리: ");
            if (lc.getMaxCsAdvantageOnLaneOpponent() != null) {
                sb.append(String.format("%.1f", lc.getMaxCsAdvantageOnLaneOpponent()));
            } else sb.append("-");
            sb.append(" | 라인전 골드우위: ");
            if (lc.getLaningPhaseGoldExpAdvantage() != null) {
                sb.append(String.format("%.0f", lc.getLaningPhaseGoldExpAdvantage()));
            } else sb.append("-");
            sb.append("\n");
        }
    }

    private String formatDiff(Integer d) {
        if (d == null) return "-";
        return (d >= 0 ? "+" : "") + d;
    }

    private void appendTimelineEventsBlock(StringBuilder sb, List<LolTimelineEventSummaryDTO> events, int maxEvents) {
        sb.append("\n[주요 타임라인]\n");
        int count = maxEvents < 0 ? events.size() : Math.min(maxEvents, events.size());

        // minute -> lines (시간 순 출력을 위해 TreeMap 사용)
        Map<Integer, List<String>> byMinute = new TreeMap<>();

        Map<String, Integer> plateGroups = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            var evt = events.get(i);
            if ("TURRET_PLATE_DESTROYED".equals(evt.getType())) {
                String key = evt.getGroupKey() != null ? evt.getGroupKey() : evt.getMinute() + "_" + (evt.getDetail() != null ? evt.getDetail() : "UNKNOWN");
                plateGroups.merge(key, 1, Integer::sum);
            } else {
                int min = evt.getMinute() != null ? evt.getMinute() : 0;
                byMinute.computeIfAbsent(min, k -> new ArrayList<>()).add(formatTimelineEventLine(evt));
            }
        }
        for (Map.Entry<String, Integer> e : plateGroups.entrySet()) {
            String key = e.getKey();
            int minute = 0;
            String laneKr = "?";
            boolean isEnemy = false; // 적 포탑 방패 채굴(우리 이득) vs 아군 포탑 방패 파괴(적이 부숨)
            if (key != null) {
                String[] parts = key.split("_", 3);
                if (parts.length >= 3) {
                    isEnemy = "ENEMY".equalsIgnoreCase(parts[0]);
                    try { minute = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                    laneKr = laneToKorean(parts[2]);
                } else if (key.matches("\\d+_.+")) {
                    int idx = key.indexOf('_');
                    try { minute = Integer.parseInt(key.substring(0, idx)); } catch (NumberFormatException ignored) {}
                    laneKr = laneToKorean(key.substring(idx + 1));
                }
            }
            String whose = isEnemy ? "적군" : "아군";
            String line = minute + "분 " + whose + " " + laneKr + " 포탑 방패 " + e.getValue() + "개 파괴";
            byMinute.computeIfAbsent(minute, k -> new ArrayList<>()).add(line);
        }
        byMinute.forEach((min, lines) -> lines.forEach(line -> sb.append(line).append("\n")));
    }

    private String positionToKorean(String position) {
        if (position == null || position.isBlank()) return "-";
        return switch (position.toUpperCase()) {
            case "TOP" -> "탑";
            case "MID", "MIDDLE" -> "미드";
            case "JUNGLE", "JGL" -> "정글";
            case "BOT", "BOTTOM", "ADC" -> "봇";
            case "SUPPORT", "SUP", "UTILITY" -> "서포터";
            default -> position;
        };
    }

    private String laneToKorean(String lane) {
        if (lane == null) return "?";
        return switch (lane.toUpperCase()) {
            case "TOP_LANE" -> "탑";
            case "MID_LANE", "MIDDLE_LANE" -> "미드";
            case "BOT_LANE", "BOTTOM_LANE" -> "봇";
            default -> lane;
        };
    }

    private String formatTimelineEventLine(LolTimelineEventSummaryDTO evt) {
        String min = evt.getMinute() != null ? evt.getMinute() + "분" : "?분";
        if ("CHAMPION_KILL".equals(evt.getType()) && evt.getPlayerRole() != null) {
            String line = min + " 챔피언 킬 (플레이어: " + evt.getPlayerRole() + ")"
                    + (evt.getPositionBrief() != null ? " " + evt.getPositionBrief() : "");
            if (evt.getDetail() != null) {
                line += " (" + evt.getDetail() + ")";
            }
            return line;
        }
        if ("LEVEL_UP".equals(evt.getType()) && evt.getDetail() != null) {
            return min + " " + evt.getDetail();
        }
        if ("ITEM_PURCHASED".equals(evt.getType()) && evt.getDetail() != null) {
            return min + " " + evt.getDetail();
        }
        if ("ELITE_MONSTER_KILL".equals(evt.getType()) && evt.getDetail() != null) {
            String[] parts = evt.getDetail().split(":", 2);
            String ownerKr = parts.length >= 2 && "ALLY".equals(parts[0]) ? "아군 " : parts.length >= 2 && "ENEMY".equals(parts[0]) ? "적군 " : "";
            String monster = parts.length >= 2 ? parts[1] : evt.getDetail();
            return min + " " + ownerKr + monster + " 처치";
        }
        if ("DRAGON_SOUL_GIVEN".equals(evt.getType()) && evt.getDetail() != null) {
            String[] parts = evt.getDetail().split(":", 2);
            if (parts.length >= 2 && "SOUL_TYPE".equals(parts[0])) {
                return min + " 드래곤 영혼 속성 결정 (" + parts[1] + ")";
            }
            String ownerKr = parts.length >= 2 && "ALLY".equals(parts[0]) ? "아군 " : parts.length >= 2 && "ENEMY".equals(parts[0]) ? "적군 " : "";
            String soul = parts.length >= 2 ? parts[1] : "드래곤영혼";
            return min + " " + ownerKr + "드래곤 영혼(" + soul + ") 획득";
        }
        if ("GAME_END".equals(evt.getType()) && evt.getDetail() != null) {
            String who = "ALLY".equals(evt.getDetail()) ? "아군" : "ENEMY".equals(evt.getDetail()) ? "적군" : "?";
            return min + " 게임 종료 (" + who + " 승리)";
        }
        if ("BUILDING_KILL".equals(evt.getType()) && evt.getDetail() != null) {
            String[] parts = evt.getDetail().split(":");
            if (parts.length >= 3) {
                String ownerKr = "ALLY".equals(parts[0]) ? "아군 " : "ENEMY".equals(parts[0]) ? "적군 " : "";
                String lane = parts[1];
                String tier = parts[2];
                String target = "억제기".equals(tier) ? "억제기" : "포탑(" + tier + ")";
                return min + " " + ownerKr + (lane.isEmpty() ? "" : lane + " ") + target + " 파괴";
            }
        }
        return min + " " + evt.getType()
                + (evt.getDetail() != null ? " " + evt.getDetail() : "");
    }

    private String toResultKorean(MatchResult result) {
        if (result == null) return "-";
        return switch (result) {
            case VICTORY -> "승리";
            case DEFEAT -> "패배";
            case DRAW -> "무승부";
        };
    }

    private String shortenPuuid(String puuid) {
        if (puuid == null || puuid.length() < 12) return puuid;
        return puuid.substring(0, 8) + "…";
    }
}
