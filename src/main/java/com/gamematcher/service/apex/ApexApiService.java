package com.gamematcher.service.apex;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Apex Legends 전적 검색 서비스
 *
 * Mozambique Here API (비공식) 사용
 * API 키 발급: https://portal.mozambiquehe.re/
 *
 * 지원 플랫폼: PC(Origin), PS4, X1
 *
 * 요청 예시:
 * {
 *   "game": "apex",
 *   "gameName": "Shroud",       ← Origin/EA 닉네임
 *   "platform": "PC",           ← PC | PS4 | X1 (기본값: PC)
 *   "count": 5
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApexApiService {

    private final RestTemplate restTemplate;

    @Value("${apex.api.key:}")
    private String apexApiKey;

    private static final String BASE_URL = "https://api.mozambiquehe.re";

    private static final Map<String, String> RANK_DISPLAY = new LinkedHashMap<>();
    static {
        RANK_DISPLAY.put("Rookie",   "🟫 루키");
        RANK_DISPLAY.put("Bronze",   "🟤 브론즈");
        RANK_DISPLAY.put("Silver",   "⬜ 실버");
        RANK_DISPLAY.put("Gold",     "🟨 골드");
        RANK_DISPLAY.put("Platinum", "🩵 플래티넘");
        RANK_DISPLAY.put("Diamond",  "💎 다이아몬드");
        RANK_DISPLAY.put("Master",   "🔮 마스터");
        RANK_DISPLAY.put("Predator", "🏆 Apex Predator");
    }

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String gameName = req.getGameName();
        if (gameName == null || gameName.isBlank()) {
            return PlayerSearchResponse.error("apex", "unknown",
                    "gameName(Origin/EA 닉네임)을 입력하세요.");
        }

        if (apexApiKey == null || apexApiKey.isBlank()) {
            return PlayerSearchResponse.error("apex", gameName,
                    "Apex API 키 미설정 → https://portal.mozambiquehe.re/ 에서 발급 후 " +
                    "application.properties 에 apex.api.key=YOUR_KEY 추가");
        }

        String platform = resolvePlatform(req.getPlatform());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apexApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 1) 플레이어 기본 정보 + 랭크
            String playerUrl = String.format(
                    "%s/bridge?version=5&platform=%s&player=%s&merge=true&removeMerged=true",
                    BASE_URL, platform, urlEncode(gameName));
            log.info("Apex 플레이어 조회: {}", playerUrl);

            @SuppressWarnings("unchecked")
            Map<String, Object> playerData = restTemplate.exchange(
                    playerUrl, HttpMethod.GET, entity, Map.class).getBody();

            if (playerData == null) {
                return PlayerSearchResponse.error("apex", gameName, "플레이어 정보를 불러올 수 없습니다.");
            }
            if (playerData.containsKey("Error")) {
                return PlayerSearchResponse.error("apex", gameName,
                        "플레이어를 찾을 수 없습니다: " + gameName + " (플랫폼: " + platform + ")");
            }

            // 기본 정보 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> global = (Map<String, Object>) playerData.get("global");
            @SuppressWarnings("unchecked")
            Map<String, Object> realtime = (Map<String, Object>) playerData.get("realtime");

            String uid         = global != null ? String.valueOf(global.getOrDefault("uid", "")) : "";
            String displayName = global != null ? (String) global.getOrDefault("name", gameName) : gameName;
            String avatarUrl   = global != null ? (String) global.get("avatar") : null;
            int    level       = global != null ? toInt(global.get("level")) : 0;

            // 랭크 정보
            String rankName = "UNRANKED";
            int    rankScore = 0;
            if (global != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rank = (Map<String, Object>) global.get("rank");
                if (rank != null) {
                    rankName  = (String) rank.getOrDefault("rankName", "Unranked");
                    rankScore = toInt(rank.get("rankScore"));
                    rankName  = RANK_DISPLAY.getOrDefault(rankName, rankName);
                }
            }

            // 2) 레전드별 통계 파싱 → MatchInfo
            @SuppressWarnings("unchecked")
            Map<String, Object> legends = (Map<String, Object>) playerData.get("legends");
            List<MatchInfo> matches = buildLegendMatches(legends);

            // 3) 커리어 통계 요약 (selected 레전드 기반)
            MatchStats stats = buildStats(matches, level);

            // 4) 현재 상태 extras
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("레벨",      level);
            extras.put("랭크 점수", rankScore + " RP");
            if (realtime != null) {
                String currentLegend = (String) realtime.get("currentLegend");
                if (currentLegend != null && !currentLegend.isBlank()) {
                    extras.put("현재 레전드", currentLegend);
                }
                boolean isOnline = Boolean.TRUE.equals(realtime.get("isOnline"))
                        || Integer.valueOf(1).equals(realtime.get("isOnline"));
                extras.put("온라인", isOnline ? "✅" : "❌");
            }

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(uid)
                    .gameName(displayName)
                    .summonerLevel(String.valueOf(level))
                    .tier(rankName)
                    .avatarUrl(avatarUrl)
                    .rawData(extras)
                    .build();

            return PlayerSearchResponse.builder()
                    .success(true)
                    .game("apex")
                    .nickname(displayName)
                    .playerInfo(playerInfo)
                    .matches(matches)
                    .stats(stats)
                    .build();

        } catch (Exception e) {
            log.error("Apex 전적 검색 오류 - {}", gameName, e);
            String msg = e.getMessage();
            if (msg != null && msg.contains("404")) {
                msg = "플레이어를 찾을 수 없습니다: " + gameName
                        + "\n· Origin/EA 닉네임이 정확한지 확인하세요."
                        + "\n· 플랫폼 확인 (PC / PS4 / X1)";
            } else if (msg != null && msg.contains("429")) {
                msg = "API 요청 한도 초과. 잠시 후 다시 시도하세요.";
            }
            return PlayerSearchResponse.error("apex", gameName, msg);
        }
    }

    @SuppressWarnings("unchecked")
    private List<MatchInfo> buildLegendMatches(Map<String, Object> legends) {
        if (legends == null) return List.of();

        Map<String, Object> all = (Map<String, Object>) legends.get("all");
        if (all == null) return List.of();

        List<MatchInfo> result = new ArrayList<>();

        for (Map.Entry<String, Object> entry : all.entrySet()) {
            String legendName = entry.getKey();
            if (!(entry.getValue() instanceof Map)) continue;

            Map<String, Object> legendData = (Map<String, Object>) entry.getValue();

            // Mozambique Here API: data 필드는 List<{key, value}> 형태
            Object dataObj = legendData.get("data");
            if (!(dataObj instanceof List)) continue;

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
            if (dataList.isEmpty()) continue;

            int kills = 0, damage = 0, wins = 0;
            for (Map<String, Object> stat : dataList) {
                String key = String.valueOf(stat.getOrDefault("key", ""));
                int val    = toInt(stat.get("value"));
                if (key.toLowerCase().contains("kill"))   kills  = val;
                if (key.toLowerCase().contains("damage")) damage = val;
                if (key.toLowerCase().contains("win"))    wins   = val;
            }

            if (kills == 0 && damage == 0) continue;

            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("레전드",   legendName);
            extras.put("총 킬",    kills);
            extras.put("총 데미지",damage);
            extras.put("총 승리",  wins + "회");

            result.add(MatchInfo.builder()
                    .matchId(legendName.toLowerCase())
                    .gameMode("배틀로얄 · " + legendName)
                    .agent(legendName)
                    .win(wins > 0)
                    .kills(kills).deaths(0).assists(0)
                    .kda(0.0)
                    .extras(extras)
                    .build());
        }

        // 킬 내림차순 정렬
        result.sort((a, b) -> Integer.compare(
                b.getKills() != null ? b.getKills() : 0,
                a.getKills() != null ? a.getKills() : 0));
        return result.stream().limit(10).collect(Collectors.toList());
    }

    private MatchStats buildStats(List<MatchInfo> matches, int level) {
        if (matches.isEmpty()) return MatchStats.builder().build();
        int totalKills = matches.stream().mapToInt(m -> m.getKills() != null ? m.getKills() : 0).sum();
        int wins       = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        String mostLegend = matches.stream()
                .filter(m -> m.getAgent() != null)
                .max(Comparator.comparingInt(m -> m.getKills() != null ? m.getKills() : 0))
                .map(MatchInfo::getAgent)
                .orElse("-");
        return MatchStats.builder()
                .totalGames(matches.size())
                .wins(wins)
                .losses(matches.size() - wins)
                .winRate(matches.size() > 0
                        ? Math.round((double) wins / matches.size() * 1000.0) / 10.0 : 0)
                .avgKills(matches.size() > 0
                        ? Math.round((double) totalKills / matches.size() * 10.0) / 10.0 : 0)
                .mostUsedChampionOrAgent(mostLegend)
                .build();
    }

    private String resolvePlatform(String platform) {
        if (platform == null) return "PC";
        return switch (platform.toUpperCase()) {
            case "PC", "ORIGIN", "EA", "STEAM" -> "PC";
            case "PS4", "PS5", "PSN"            -> "PS4";
            case "X1", "XBOX"                   -> "X1";
            case "SWITCH", "NSW"                -> "Switch";
            default                              -> "PC";
        };
    }

    private int toInt(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }
}

