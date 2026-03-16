package com.gamematcher.service.pubg;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PUBG 공식 API 전적 검색 서비스
 *
 * API 키 발급: https://developer.pubg.com/
 *   - 무료: 10req/min
 *   - 키 발급 후 application.properties 에 pubg.api.key 설정
 *
 * 요청 예시:
 * {
 *   "game": "pubg",
 *   "gameName": "PlayerNickname",   ← PUBG 인게임 닉네임 (#태그 없음)
 *   "platform": "steam",            ← steam | kakao | psn | xbox (기본값: steam)
 *   "count": 5
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PubgApiService {

    private final RestTemplate restTemplate;

    @Value("${pubg.api.key:}")
    private String pubgApiKey;

    private static final String BASE = "https://api.pubg.com/shards";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> PLATFORM_SHARD = Map.of(
            "steam",  "steam",
            "kakao",  "kakao",
            "psn",    "psn",
            "xbox",   "xbox",
            "stadia", "stadia"
    );

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String nickname = req.getGameName();
        if (nickname == null || nickname.isBlank()) {
            return PlayerSearchResponse.error("pubg", "unknown", "gameName(닉네임)을 입력하세요.");
        }

        if (pubgApiKey == null || pubgApiKey.isEmpty()) {
            return PlayerSearchResponse.error("pubg", nickname,
                    "PUBG API 키 미설정 → https://developer.pubg.com/ 에서 발급 후 " +
                    "application.properties 에 pubg.api.key=YOUR_KEY 추가");
        }

        String platform = req.getPlatform() != null
                ? PLATFORM_SHARD.getOrDefault(req.getPlatform().toLowerCase(), "steam")
                : "steam";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + pubgApiKey);
        headers.set("Accept", "application/vnd.api+json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // ── 1) 플레이어 조회 ──────────────────────────────
            String playerUrl = String.format(
                    "%s/%s/players?filter[playerNames]=%s",
                    BASE, platform, urlEncode(nickname)
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> playerResp = restTemplate
                    .exchange(playerUrl, HttpMethod.GET, entity, Map.class).getBody();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> playerData = playerResp != null
                    ? (List<Map<String, Object>>) playerResp.get("data") : null;
            if (playerData == null || playerData.isEmpty())
                throw new RuntimeException("플레이어를 찾을 수 없습니다: " + nickname);

            Map<String, Object> player  = playerData.get(0);
            String accountId            = (String) player.get("id");

            @SuppressWarnings("unchecked")
            Map<String, Object> attrs   = (Map<String, Object>) player.get("attributes");
            String displayName          = attrs != null
                    ? (String) attrs.getOrDefault("name", nickname) : nickname;

            // ── 2) 최근 매치 ID 목록 ──────────────────────────
            @SuppressWarnings("unchecked")
            Map<String, Object> rels    = (Map<String, Object>) player.get("relationships");
            @SuppressWarnings("unchecked")
            Map<String, Object> mRel    = rels != null ? (Map<String, Object>) rels.get("matches") : null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matchRefs = mRel != null
                    ? (List<Map<String, Object>>) mRel.get("data") : List.of();

            int limit = Math.min(req.getCount(), Math.min(matchRefs.size(), 20));
            List<String> matchIds = matchRefs.stream()
                    .limit(limit)
                    .map(m -> (String) m.get("id"))
                    .collect(Collectors.toList());

            // ── 3) 매치 상세 조회 ──────────────────────────────
            List<MatchInfo> matches = new ArrayList<>();
            for (String matchId : matchIds) {
                try {
                    MatchInfo info = fetchMatchDetail(matchId, accountId, platform, entity);
                    if (info != null) matches.add(info);
                    Thread.sleep(200); // rate limit 대비
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.warn("PUBG 매치 상세 조회 실패 - {}", matchId, e);
                }
            }

            // ── 4) 시즌 랭크 (실패해도 무시) ──────────────────
            String tier = "UNRANKED";
            try { tier = fetchCurrentRank(accountId, platform, entity); }
            catch (Exception e) { log.warn("PUBG 랭크 조회 실패 (무시)", e); }

            MatchStats stats = buildStats(matches);

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(accountId)
                    .gameName(displayName)
                    .tier(tier)
                    .build();

            return PlayerSearchResponse.builder()
                    .success(true)
                    .game("pubg")
                    .nickname(displayName)
                    .playerInfo(playerInfo)
                    .matches(matches)
                    .stats(stats)
                    .build();

        } catch (Exception e) {
            log.error("PUBG 전적 검색 오류 - {}", nickname, e);
            return PlayerSearchResponse.error("pubg", nickname, e.getMessage());
        }
    }

    // ── 매치 상세 ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private MatchInfo fetchMatchDetail(String matchId, String accountId,
                                        String platform, HttpEntity<Void> entity) {
        String url = String.format("%s/%s/matches/%s", BASE, platform, matchId);
        Map<String, Object> resp = restTemplate
                .exchange(url, HttpMethod.GET, entity, Map.class).getBody();
        if (resp == null) return null;

        Map<String, Object> matchData = (Map<String, Object>) resp.get("data");
        List<Map<String, Object>> included = (List<Map<String, Object>>) resp.get("included");
        if (matchData == null || included == null) return null;

        // 매치 기본 정보
        Map<String, Object> matchAttrs = (Map<String, Object>) matchData.get("attributes");
        String gameMode  = matchAttrs != null ? (String) matchAttrs.getOrDefault("gameMode", "") : "";
        String mapName   = matchAttrs != null ? (String) matchAttrs.getOrDefault("mapName", "") : "";
        String createdAt = matchAttrs != null ? (String) matchAttrs.get("createdAt") : "";

        // included 에서 내 participant 찾기 (playerId == accountId)
        Map<String, Object> myStats = null;
        for (Map<String, Object> item : included) {
            if (!"participant".equals(item.get("type"))) continue;
            Map<String, Object> pAttrs = (Map<String, Object>) item.get("attributes");
            if (pAttrs == null) continue;
            Map<String, Object> st = (Map<String, Object>) pAttrs.get("stats");
            if (st == null) continue;
            if (accountId.equals(st.get("playerId"))) { myStats = st; break; }
        }
        if (myStats == null) return null;

        int    kills        = toInt(myStats.get("kills"));
        int    assists      = toInt(myStats.get("assists"));
        int    winPlace     = toInt(myStats.get("winPlace"));   // 순위
        boolean win         = winPlace == 1;
        int    headshotK    = toInt(myStats.get("headshotKills"));
        double damage       = toDouble(myStats.get("damageDealt"));
        double survive      = toDouble(myStats.get("timeSurvived")); // 초
        double walkDist     = toDouble(myStats.get("walkDistance"));
        double rideDist     = toDouble(myStats.get("rideDistance"));

        String playedAt = "";
        try {
            if (createdAt != null && !createdAt.isEmpty())
                playedAt = OffsetDateTime.parse(createdAt).toLocalDateTime().format(FORMATTER);
        } catch (Exception ignored) {}

        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("순위",      winPlace + "위");
        extras.put("데미지",    String.format("%.0f", damage));
        extras.put("헤드샷킬",  headshotK);
        extras.put("생존시간",  String.format("%.0f분", survive / 60));
        extras.put("이동거리",  String.format("%.0fm", walkDist + rideDist));
        extras.put("맵",       translateMap(mapName));

        return MatchInfo.builder()
                .matchId(matchId)
                .gameMode(formatGameMode(gameMode))
                .win(win)
                .kills(kills)
                .deaths(winPlace)   // PUBG 는 deaths 대신 순위 저장
                .assists(assists)
                .kda((double) kills + assists)  // K+A 표기
                .playtime((int) survive)
                .playedAt(playedAt)
                .extras(extras)
                .build();
    }

    // ── 시즌 랭크 ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String fetchCurrentRank(String accountId, String platform,
                                     HttpEntity<Void> entity) {
        // 현재 시즌 ID
        String seasonUrl = String.format("%s/%s/seasons", BASE, platform);
        Map<String, Object> seasonResp = restTemplate
                .exchange(seasonUrl, HttpMethod.GET, entity, Map.class).getBody();
        List<Map<String, Object>> seasons = seasonResp != null
                ? (List<Map<String, Object>>) seasonResp.get("data") : null;
        if (seasons == null || seasons.isEmpty()) return "N/A";

        String currentSeasonId = seasons.stream()
                .filter(s -> {
                    Map<String, Object> a = (Map<String, Object>) s.get("attributes");
                    return a != null && Boolean.TRUE.equals(a.get("isCurrentSeason"));
                })
                .map(s -> (String) s.get("id"))
                .findFirst().orElse(null);
        if (currentSeasonId == null) return "N/A";

        // 랭크 통계
        String rankUrl = String.format(
                "%s/%s/players/%s/seasons/%s/ranked",
                BASE, platform, accountId, currentSeasonId
        );
        Map<String, Object> rankResp = restTemplate
                .exchange(rankUrl, HttpMethod.GET, entity, Map.class).getBody();
        Map<String, Object> rankData = rankResp != null
                ? (Map<String, Object>) rankResp.get("data") : null;
        if (rankData == null) return "UNRANKED";

        Map<String, Object> rankAttrs = (Map<String, Object>) rankData.get("attributes");
        if (rankAttrs == null) return "UNRANKED";

        Map<String, Object> modeStats =
                (Map<String, Object>) rankAttrs.get("rankedGameModeStats");
        if (modeStats == null) return "UNRANKED";

        // squad-fpp 우선, 없으면 squad
        Map<String, Object> mode = (Map<String, Object>) modeStats.get("squad-fpp");
        if (mode == null) mode = (Map<String, Object>) modeStats.get("squad");
        if (mode == null) return "UNRANKED";

        Map<String, Object> currentTier = (Map<String, Object>) mode.get("currentTier");
        if (currentTier == null) return "UNRANKED";

        String tier    = (String) currentTier.getOrDefault("tier",    "UNRANKED");
        String subTier = (String) currentTier.getOrDefault("subTier", "");
        return tier + (subTier.isEmpty() ? "" : " " + subTier);
    }

    // ── 유틸 ──────────────────────────────────────────────────────────────────

    private MatchStats buildStats(List<MatchInfo> matches) {
        if (matches.isEmpty()) return MatchStats.builder().build();
        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        double avgK    = matches.stream().mapToInt(m -> nvl(m.getKills())).average().orElse(0);
        double avgA    = matches.stream().mapToInt(m -> nvl(m.getAssists())).average().orElse(0);
        double avgRank = matches.stream().mapToInt(m -> nvl(m.getDeaths())).average().orElse(0);
        return MatchStats.builder()
                .totalGames(matches.size())
                .wins(wins).losses(matches.size() - wins)
                .winRate(round1(100.0 * wins / matches.size()))
                .avgKills(round1(avgK)).avgAssists(round1(avgA))
                .avgKda(round1(avgRank))  // 평균 순위
                .build();
    }

    private String formatGameMode(String raw) {
        if (raw == null) return "";
        return switch (raw.toLowerCase()) {
            case "squad"     -> "스쿼드 (3인칭)";
            case "squad-fpp" -> "스쿼드 (1인칭)";
            case "duo"       -> "듀오 (3인칭)";
            case "duo-fpp"   -> "듀오 (1인칭)";
            case "solo"      -> "솔로 (3인칭)";
            case "solo-fpp"  -> "솔로 (1인칭)";
            default          -> raw;
        };
    }

    private String translateMap(String raw) {
        if (raw == null) return "";
        return switch (raw) {
            case "Baltic_Main"    -> "에란겔";
            case "Erangel_Main"   -> "에란겔";
            case "Desert_Main"    -> "미라마";
            case "Savage_Main"    -> "사녹";
            case "DihorOtok_Main" -> "비켄디";
            case "Summerland_Main"-> "카라킨";
            case "Tiger_Main"     -> "태이고";
            case "Kiki_Main"      -> "데스턴";
            case "Neon_Main"      -> "론도";
            default               -> raw;
        };
    }

    private int toInt(Object o)        { return o instanceof Number n ? n.intValue()    : 0;   }
    private double toDouble(Object o)  { return o instanceof Number n ? n.doubleValue() : 0.0; }
    private int nvl(Integer i)         { return i != null ? i : 0; }
    private double round1(double v)    { return Math.round(v * 10.0) / 10.0; }
    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }
}

