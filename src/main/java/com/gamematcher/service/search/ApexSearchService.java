package com.gamematcher.service.search;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Apex Legends 전적 검색 서비스
 *
 * Tracker.gg API 사용
 * API 키 발급: https://tracker.gg/developers
 *
 * 지원 플랫폼: origin(PC), psn, xbl
 *
 * 요청 예시:
 * {
 *   "game": "apex",
 *   "gameName": "iiTzTimmy",
 *   "platform": "origin"   <- origin | psn | xbl (기본값: origin)
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApexSearchService {

    private final RestTemplate restTemplate;

    @Value("${tracker.api.key:}")
    private String trackerApiKey;

    private static final String BASE_URL = "https://public-api.tracker.gg/v2/apex/standard";

    @PostConstruct
    void logTrackerApiKeyStatus() {
        log.info("Apex Tracker API key loaded: present={}, length={}, masked={}",
                trackerApiKey != null && !trackerApiKey.isBlank(),
                trackerApiKey == null ? 0 : trackerApiKey.length(),
                maskKey(trackerApiKey));
    }

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

        if (trackerApiKey == null || trackerApiKey.isBlank()) {
            return PlayerSearchResponse.error("apex", gameName,
                    "Tracker.gg API 키 미설정\n"
                            + "· https://tracker.gg/developers 에서 발급 후\n"
                            + "· application.properties: tracker.api.key=YOUR_KEY");
        }

        String platform = resolvePlatform(req.getPlatform());

        try {
            log.info("Apex search request: gameName={}, platform={}, trackerKeyPresent={}, trackerKeyLength={}, trackerKeyMasked={}",
                    gameName, platform,
                    trackerApiKey != null && !trackerApiKey.isBlank(),
                    trackerApiKey == null ? 0 : trackerApiKey.length(),
                    maskKey(trackerApiKey));
            HttpHeaders headers = new HttpHeaders();
            headers.set("TRN-Api-Key", trackerApiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 프로필 조회
            String profileUrl = String.format(
                    "%s/profile/%s/%s",
                    BASE_URL, platform, urlEncode(gameName));
            log.info("Apex (tracker.gg) 플레이어 조회: {}", profileUrl);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    profileUrl, HttpMethod.GET, entity, Map.class).getBody();

            if (response == null) {
                return PlayerSearchResponse.error("apex", gameName, "플레이어 정보를 불러올 수 없습니다.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                return PlayerSearchResponse.error("apex", gameName, "플레이어 데이터가 없습니다.");
            }

            // 플레이어 정보 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> platformInfo = (Map<String, Object>) data.get("platformInfo");
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) data.get("userInfo");

            String displayName = platformInfo != null
                    ? (String) platformInfo.getOrDefault("platformUserHandle", gameName) : gameName;
            String avatarUrl = platformInfo != null
                    ? (String) platformInfo.get("avatarUrl") : null;

            // segments 파싱 - overview + legend 별 통계
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segments =
                    (List<Map<String, Object>>) data.get("segments");

            // 전체 통계 (overview segment)
            Map<String, Object> overviewStats = findSegmentStats(segments, "overview");

            String rankName   = extractDisplayValue(overviewStats, "rankScore");
            int    level      = extractIntValue(overviewStats, "level");
            double kills      = extractDoubleValue(overviewStats, "kills");
            double damage     = extractDoubleValue(overviewStats, "damage");
            int    wins       = extractIntValue(overviewStats, "wins");
            int    games      = extractIntValue(overviewStats, "matches");
            double winRate    = games > 0 ? Math.round((double) wins / games * 1000.0) / 10.0 : 0;
            double kd         = extractDoubleValue(overviewStats, "kd");

            // 랭크 표시
            String rankDisplay = "UNRANKED";
            if (rankName != null && !rankName.isBlank() && !rankName.equals("0")) {
                for (Map.Entry<String, String> e : RANK_DISPLAY.entrySet()) {
                    if (rankName.toLowerCase().contains(e.getKey().toLowerCase())) {
                        rankDisplay = e.getValue();
                        break;
                    }
                }
                if (rankDisplay.equals("UNRANKED")) rankDisplay = rankName;
            }

            // 레전드별 매치 목록
            List<MatchInfo> matches = buildLegendMatches(segments);

            // 추가 정보
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("레벨",       level);
            extras.put("총 킬",      (long) kills);
            extras.put("총 데미지",  (long) damage);
            extras.put("총 승리",    wins + "회");
            if (kd > 0) extras.put("K/D", String.format("%.2f", kd));

            MatchStats stats = MatchStats.builder()
                    .totalGames(games)
                    .wins(wins)
                    .losses(Math.max(0, games - wins))
                    .winRate(winRate)
                    .avgKda(kd)
                    .build();

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .gameName(displayName)
                    .summonerLevel(level > 0 ? String.valueOf(level) : null)
                    .tier(rankDisplay)
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

        } catch (HttpStatusCodeException e) {
            log.error("Apex tracker API HTTP error - gameName={}, status={}, response={}",
                    gameName, e.getStatusCode(), e.getResponseBodyAsString(), e);
            String msg = e.getMessage();
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                msg = "Tracker.gg API 키가 서버에서 거부되었습니다.\n"
                        + "서버가 읽은 키: " + maskKey(trackerApiKey) + "\n"
                        + "키 길이: " + (trackerApiKey == null ? 0 : trackerApiKey.length()) + "\n"
                        + "Tracker 개발자 콘솔에서 Apex 접근 권한과 활성 상태를 확인해 주세요.";
            } else if (e.getStatusCode().value() == 404) {
                msg = "플레이어를 찾을 수 없습니다: " + gameName
                        + "\nOrigin/EA 닉네임과 플랫폼(origin / psn / xbl)을 다시 확인해 주세요.";
            } else if (e.getStatusCode().value() == 429) {
                msg = "API 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            }
            return PlayerSearchResponse.error("apex", gameName, msg);
        } catch (Exception e) {
            log.error("Apex 전적 검색 오류 - {}", gameName, e);
            String msg = e.getMessage();
            if (msg != null && (msg.contains("401") || msg.contains("403"))) {
                msg = "Tracker.gg API 키가 유효하지 않습니다.\n"
                        + "· https://tracker.gg/developers 에서 키를 확인하세요.\n"
                        + "· application.properties: tracker.api.key=YOUR_KEY";
            } else if (msg != null && msg.contains("404")) {
                msg = "플레이어를 찾을 수 없습니다: " + gameName
                        + "\n· Origin/EA 닉네임이 정확한지 확인하세요."
                        + "\n· 플랫폼 확인 (origin / psn / xbl)";
            } else if (msg != null && msg.contains("429")) {
                msg = "API 요청 한도 초과. 잠시 후 다시 시도하세요.";
            }
            return PlayerSearchResponse.error("apex", gameName, msg);
        }
    }

    /** segments 에서 type 이 일치하는 항목의 stats 반환 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findSegmentStats(List<Map<String, Object>> segments, String type) {
        if (segments == null) return Map.of();
        for (Map<String, Object> seg : segments) {
            if (type.equals(seg.get("type"))) {
                Object s = seg.get("stats");
                if (s instanceof Map) return (Map<String, Object>) s;
            }
        }
        return Map.of();
    }

    /** segments 에서 레전드별 MatchInfo 목록 구성 */
    @SuppressWarnings("unchecked")
    private List<MatchInfo> buildLegendMatches(List<Map<String, Object>> segments) {
        if (segments == null) return List.of();
        List<MatchInfo> result = new ArrayList<>();

        for (Map<String, Object> seg : segments) {
            if (!"legend".equals(seg.get("type"))) continue;

            Map<String, Object> metadata = (Map<String, Object>) seg.get("metadata");
            String legendName = metadata != null ? (String) metadata.getOrDefault("name", "Unknown") : "Unknown";
            String imgUrl     = metadata != null ? (String) metadata.get("imageUrl") : null;

            Map<String, Object> stats = seg.get("stats") instanceof Map
                    ? (Map<String, Object>) seg.get("stats") : Map.of();

            int kills  = extractIntValue(stats, "kills");
            int damage = extractIntValue(stats, "damage");
            int wins   = extractIntValue(stats, "wins");
            int games  = extractIntValue(stats, "matches");
            double kd  = extractDoubleValue(stats, "kd");

            if (kills == 0 && damage == 0 && games == 0) continue;

            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("레전드",   legendName);
            extras.put("총 킬",    kills);
            extras.put("총 데미지",damage);
            extras.put("총 승리",  wins + "회");
            if (kd > 0) extras.put("K/D", String.format("%.2f", kd));

            result.add(MatchInfo.builder()
                    .matchId(legendName.toLowerCase())
                    .gameMode("배틀로얄 · " + legendName)
                    .agent(legendName)
                    .win(wins > 0)
                    .kills(kills).deaths(0).assists(0)
                    .kda(kd)
                    .extras(extras)
                    .build());
        }

        // 킬 내림차순
        result.sort((a, b) -> Integer.compare(
                b.getKills() != null ? b.getKills() : 0,
                a.getKills() != null ? a.getKills() : 0));
        return result.stream().limit(10).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private String extractDisplayValue(Map<String, Object> stats, String key) {
        if (stats == null) return null;
        Object stat = stats.get(key);
        if (!(stat instanceof Map)) return null;
        Map<String, Object> m = (Map<String, Object>) stat;
        Object metadata = m.get("metadata");
        if (metadata instanceof Map) {
            Object rankName = ((Map<String, Object>) metadata).get("rankName");
            if (rankName instanceof String s && !s.isBlank()) return s;
        }
        Object disp = m.get("displayValue");
        return disp instanceof String s ? s : null;
    }

    @SuppressWarnings("unchecked")
    private int extractIntValue(Map<String, Object> stats, String key) {
        if (stats == null) return 0;
        Object stat = stats.get(key);
        if (!(stat instanceof Map)) return 0;
        Object val = ((Map<String, Object>) stat).get("value");
        return val instanceof Number n ? n.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private double extractDoubleValue(Map<String, Object> stats, String key) {
        if (stats == null) return 0;
        Object stat = stats.get(key);
        if (!(stat instanceof Map)) return 0;
        Object val = ((Map<String, Object>) stat).get("value");
        return val instanceof Number n ? Math.round(n.doubleValue() * 100.0) / 100.0 : 0;
    }

    private String resolvePlatform(String platform) {
        if (platform == null) return "origin";
        return switch (platform.toUpperCase()) {
            case "PC", "ORIGIN", "EA", "STEAM" -> "origin";
            case "PS4", "PS5", "PSN"            -> "psn";
            case "X1", "XBOX", "XBL"            -> "xbl";
            default                              -> "origin";
        };
    }

    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }

    private String maskKey(String key) {
        if (key == null) return "(null)";
        String trimmed = key.trim();
        if (trimmed.isEmpty()) return "(blank)";
        if (trimmed.length() <= 8) return "****";
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}
