package com.gamematcher.service.search;

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
 * Counter-Strike 2 전적 검색 서비스
 *
 * Steam Web API 사용
 * API 키 발급: https://steamcommunity.com/dev/apikey
 *
 * CS2 App ID: 730
 *
 * 요청 예시:
 * {
 *   "game": "cs2",
 *   "gameName": "76561198000000000",  ← Steam64 ID 또는 Vanity URL
 *   "count": 5
 * }
 *
 * 주의: Steam API는 최근 매치 상세 정보를 직접 제공하지 않음.
 *       GetUserStatsForGame API로 누적 통계 조회 후 매치 이력 형태로 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cs2SearchService {

    private final RestTemplate restTemplate;

    @Value("${steam.api.key:}")
    private String steamApiKey;

    private static final String STEAM_API = "https://api.steampowered.com";
    private static final int CS2_APP_ID = 730;

    // CS2 주요 스탯 키
    private static final Map<String, String> STAT_LABELS = new LinkedHashMap<>();
    static {
        STAT_LABELS.put("total_kills",          "총 킬");
        STAT_LABELS.put("total_deaths",         "총 데스");
        STAT_LABELS.put("total_assists",        "총 어시스트");
        STAT_LABELS.put("total_wins",           "총 승리");
        STAT_LABELS.put("total_rounds_played",  "총 라운드");
        STAT_LABELS.put("total_headshot_kills", "헤드샷 킬");
        STAT_LABELS.put("total_mvps",           "총 MVP");
        STAT_LABELS.put("total_damage_done",    "총 데미지");
        STAT_LABELS.put("total_matches_played", "총 매치");
        STAT_LABELS.put("total_wins_pistolround", "피스톨 라운드 승");
    }

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String identifier = req.getSteamId() != null ? req.getSteamId() : req.getGameName();
        if (identifier == null || identifier.isBlank()) {
            return PlayerSearchResponse.error("cs2", "unknown",
                    "steamId 또는 gameName(Steam64 ID / Vanity URL)을 입력하세요.");
        }

        if (steamApiKey == null || steamApiKey.isBlank()) {
            return PlayerSearchResponse.error("cs2", identifier,
                    "Steam API 키 미설정 → https://steamcommunity.com/dev/apikey 에서 발급 후 " +
                    "application.properties 에 steam.api.key=YOUR_KEY 추가");
        }

        try {
            // 1) Steam64 ID 확인 (숫자가 아니면 Vanity URL 변환)
            String steam64Id = resolveSteamId(identifier);

            // 2) 프로필 정보
            String profileUrl = String.format(
                    "%s/ISteamUser/GetPlayerSummaries/v2/?key=%s&steamids=%s",
                    STEAM_API, steamApiKey, steam64Id);
            @SuppressWarnings("unchecked")
            Map<String, Object> profileResp = restTemplate.getForObject(profileUrl, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> profileResponse = profileResp != null
                    ? (Map<String, Object>) profileResp.get("response") : null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> players = profileResponse != null
                    ? (List<Map<String, Object>>) profileResponse.get("players") : null;

            String displayName = identifier;
            String avatarUrl   = null;
            if (players != null && !players.isEmpty()) {
                Map<String, Object> p = players.get(0);
                displayName = (String) p.getOrDefault("personaname", identifier);
                avatarUrl   = (String) p.get("avatarmedium");
            }

            // 3) CS2 통계 조회
            String statsUrl = String.format(
                    "%s/ISteamUserStats/GetUserStatsForGame/v2/?key=%s&steamid=%s&appid=%d",
                    STEAM_API, steamApiKey, steam64Id, CS2_APP_ID);
            @SuppressWarnings("unchecked")
            Map<String, Object> statsResp = restTemplate.getForObject(statsUrl, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> playerstats = statsResp != null
                    ? (Map<String, Object>) statsResp.get("playerstats") : null;

            if (playerstats == null) {
                return PlayerSearchResponse.error("cs2", displayName,
                        "CS2 통계를 불러올 수 없습니다. 프로필이 비공개이거나 CS2를 플레이하지 않았을 수 있습니다.");
            }

            // stats 배열 → Map
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> statsList =
                    (List<Map<String, Object>>) playerstats.get("stats");
            Map<String, Long> statsMap = new HashMap<>();
            if (statsList != null) {
                for (Map<String, Object> stat : statsList) {
                    String name = (String) stat.get("name");
                    Object value = stat.get("value");
                    if (name != null && value instanceof Number n) {
                        statsMap.put(name, n.longValue());
                    }
                }
            }

            // 4) 핵심 통계 추출
            long totalKills    = statsMap.getOrDefault("total_kills", 0L);
            long totalDeaths   = statsMap.getOrDefault("total_deaths", 0L);
            long totalWins     = statsMap.getOrDefault("total_wins", 0L);
            long totalMatches  = statsMap.getOrDefault("total_matches_played", 0L);
            long totalHs       = statsMap.getOrDefault("total_headshot_kills", 0L);
            long totalMvps     = statsMap.getOrDefault("total_mvps", 0L);
            long totalAssists  = statsMap.getOrDefault("total_assists", 0L);
            long totalRounds   = statsMap.getOrDefault("total_rounds_played", 0L);

            double kd = totalDeaths > 0
                    ? Math.round((double) totalKills / totalDeaths * 100.0) / 100.0 : totalKills;
            double winRate = totalMatches > 0
                    ? Math.round((double) totalWins / totalMatches * 1000.0) / 10.0 : 0;
            double hsRate = totalKills > 0
                    ? Math.round((double) totalHs / totalKills * 1000.0) / 10.0 : 0;

            // 5) 즐겨 찾는 맵 정보
            String favoriteMap = findFavoriteMap(statsMap);

            // 6) 티어 (K/D 기반 추정)
            String tier = estimateTier(kd, winRate);

            // 7) MatchInfo 형태로 변환 (통계 → 가상 요약 카드)
            List<MatchInfo> matches = buildMatchInfoFromStats(statsMap, totalMatches);

            // 8) 통계 extras
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("헤드샷률",    hsRate + "%");
            extras.put("총 MVP",      totalMvps);
            extras.put("총 라운드",   totalRounds);
            extras.put("즐겨찾는 맵", favoriteMap);

            MatchStats stats = MatchStats.builder()
                    .totalGames((int) totalMatches)
                    .wins((int) totalWins)
                    .losses((int) Math.max(0, totalMatches - totalWins))
                    .winRate(winRate)
                    .avgKills(totalMatches > 0
                            ? Math.round((double) totalKills / totalMatches * 10.0) / 10.0 : 0)
                    .avgDeaths(totalMatches > 0
                            ? Math.round((double) totalDeaths / totalMatches * 10.0) / 10.0 : 0)
                    .avgAssists(totalMatches > 0
                            ? Math.round((double) totalAssists / totalMatches * 10.0) / 10.0 : 0)
                    .avgKda(kd)
                    .mostUsedChampionOrAgent(favoriteMap)
                    .build();

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(steam64Id)
                    .gameName(displayName)
                    .tier(tier)
                    .avatarUrl(avatarUrl)
                    .rawData(extras)
                    .build();

            return PlayerSearchResponse.builder()
                    .success(true)
                    .game("cs2")
                    .nickname(displayName)
                    .playerInfo(playerInfo)
                    .matches(matches)
                    .stats(stats)
                    .build();

        } catch (Exception e) {
            log.error("CS2 전적 검색 오류 - {}", identifier, e);
            String msg = e.getMessage();
            if (msg != null && msg.contains("403")) {
                msg = "CS2 통계가 비공개입니다. Steam 프로필 → 게임 세부 정보를 공개로 설정하세요.";
            } else if (msg != null && msg.contains("404")) {
                msg = "플레이어를 찾을 수 없습니다: " + identifier;
            }
            return PlayerSearchResponse.error("cs2", identifier, msg);
        }
    }

    /** Steam64 ID 변환 (숫자 → 그대로, 아니면 Vanity URL 변환 시도) */
    private String resolveSteamId(String identifier) {
        if (identifier.matches("\\d{17}")) return identifier;
        String url = String.format(
                "%s/ISteamUser/ResolveVanityURL/v1/?key=%s&vanityurl=%s",
                STEAM_API, steamApiKey, identifier);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = resp != null ? (Map<String, Object>) resp.get("response") : null;
        if (response != null && Integer.valueOf(1).equals(response.get("success"))) {
            return (String) response.get("steamid");
        }
        return identifier;
    }

    /** 맵별 승리수에서 가장 많이 플레이한 맵 찾기 */
    private String findFavoriteMap(Map<String, Long> statsMap) {
        Map<String, String> mapNames = Map.of(
                "total_wins_map_de_dust2",    "먼지 2 (Dust2)",
                "total_wins_map_de_mirage",   "신기루 (Mirage)",
                "total_wins_map_de_inferno",  "화염 (Inferno)",
                "total_wins_map_de_nuke",     "핵시설 (Nuke)",
                "total_wins_map_de_overpass", "고가도로 (Overpass)",
                "total_wins_map_de_ancient",  "고대 (Ancient)",
                "total_wins_map_de_anubis",   "아누비스 (Anubis)",
                "total_wins_map_de_vertigo",  "고층빌딩 (Vertigo)"
        );
        return mapNames.entrySet().stream()
                .max(Comparator.comparingLong(e -> statsMap.getOrDefault(e.getKey(), 0L)))
                .map(Map.Entry::getValue)
                .orElse("-");
    }

    /** K/D + 승률 기반 티어 추정 */
    private String estimateTier(double kd, double winRate) {
        if (kd >= 1.5 && winRate >= 55) return "Global Elite";
        if (kd >= 1.3 && winRate >= 52) return "Supreme Master";
        if (kd >= 1.1 && winRate >= 50) return "Legendary Eagle";
        if (kd >= 1.0 && winRate >= 48) return "Distinguished Master";
        if (kd >= 0.9)                  return "Double AK";
        if (kd >= 0.7)                  return "Gold Nova";
        return "Silver";
    }

    /** 누적 통계 → 맵별 요약 MatchInfo 목록 */
    @SuppressWarnings("unchecked")
    private List<MatchInfo> buildMatchInfoFromStats(Map<String, Long> statsMap, long totalMatches) {
        List<MatchInfo> result = new ArrayList<>();

        Map<String, String> mapNames = new LinkedHashMap<>();
        mapNames.put("de_dust2",    "먼지 2 (Dust2)");
        mapNames.put("de_mirage",   "신기루 (Mirage)");
        mapNames.put("de_inferno",  "화염 (Inferno)");
        mapNames.put("de_nuke",     "핵시설 (Nuke)");
        mapNames.put("de_overpass", "고가도로 (Overpass)");
        mapNames.put("de_ancient",  "고대 (Ancient)");
        mapNames.put("de_anubis",   "아누비스 (Anubis)");
        mapNames.put("de_vertigo",  "고층빌딩 (Vertigo)");

        for (Map.Entry<String, String> entry : mapNames.entrySet()) {
            String key     = entry.getKey();
            String mapName = entry.getValue();
            long wins      = statsMap.getOrDefault("total_wins_map_" + key, 0L);
            long rounds    = statsMap.getOrDefault("total_rounds_map_" + key, 0L);
            if (wins == 0 && rounds == 0) continue;

            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("맵",      mapName);
            extras.put("승리",    wins + "회");
            extras.put("라운드",  rounds + "R");

            result.add(MatchInfo.builder()
                    .matchId(key)
                    .gameMode("경쟁전 · " + mapName)
                    .agent(mapName)
                    .win(wins > 0)
                    .kills(0).deaths(0).assists(0)
                    .kda(0.0)
                    .extras(extras)
                    .build());
        }

        // 맵 정보가 없으면 전체 통계 카드 하나 생성
        if (result.isEmpty() && totalMatches > 0) {
            long k = statsMap.getOrDefault("total_kills", 0L);
            long d = statsMap.getOrDefault("total_deaths", 0L);
            long w = statsMap.getOrDefault("total_wins", 0L);
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("총 킬",   k);
            extras.put("총 데스", d);
            extras.put("총 승리", w + "회");
            result.add(MatchInfo.builder()
                    .matchId("career")
                    .gameMode("커리어 통계")
                    .win(true)
                    .kills((int) k).deaths((int) d).assists(0)
                    .kda(d > 0 ? Math.round((double) k / d * 100.0) / 100.0 : k)
                    .extras(extras)
                    .build());
        }
        return result;
    }
}
