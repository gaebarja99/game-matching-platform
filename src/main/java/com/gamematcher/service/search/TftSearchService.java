package com.gamematcher.service.search;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TFT 전적 검색
 * - tft/summoner/v1/summoners/by-name 우선 사용 (개인 개발 키 호환)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TftSearchService {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key:}")
    private String riotApiKey;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> PLATFORM_NORMALIZE = new HashMap<>();
    static {
        PLATFORM_NORMALIZE.put("kr",    "kr");   PLATFORM_NORMALIZE.put("kr1",   "kr");
        PLATFORM_NORMALIZE.put("jp",    "jp1");  PLATFORM_NORMALIZE.put("jp1",   "jp1");
        PLATFORM_NORMALIZE.put("na",    "na1");  PLATFORM_NORMALIZE.put("na1",   "na1");
        PLATFORM_NORMALIZE.put("euw",   "euw1"); PLATFORM_NORMALIZE.put("euw1",  "euw1");
        PLATFORM_NORMALIZE.put("br",    "br1");  PLATFORM_NORMALIZE.put("br1",   "br1");
        PLATFORM_NORMALIZE.put("la1",   "la1");  PLATFORM_NORMALIZE.put("la2",   "la2");
    }

    private static final Map<String, String> REGION_ROUTING = new HashMap<>();
    static {
        REGION_ROUTING.put("kr",    "asia");  REGION_ROUTING.put("jp1",   "asia");
        REGION_ROUTING.put("na1",   "americas"); REGION_ROUTING.put("la1", "americas");
        REGION_ROUTING.put("la2",   "americas"); REGION_ROUTING.put("br1", "americas");
        REGION_ROUTING.put("euw1",  "europe"); REGION_ROUTING.put("eune1","europe");
        REGION_ROUTING.put("tr1",   "europe"); REGION_ROUTING.put("ru",   "europe");
    }

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String nickname = req.getGameName() + "#" + req.getTagLine();

        if (riotApiKey == null || riotApiKey.isBlank()) {
            return PlayerSearchResponse.error("tft", nickname, "Riot API 키가 설정되지 않았습니다.");
        }

        try {
            String rawRegion = resolveRegionHint(req);
            String platform  = PLATFORM_NORMALIZE.getOrDefault(rawRegion, rawRegion);
            String routing   = REGION_ROUTING.getOrDefault(platform, "asia");

            log.info("TFT 검색 - gameName={}, tagLine={}, platform={}, routing={}",
                    req.getGameName(), req.getTagLine(), platform, routing);

            // ── STEP 1: TFT Summoner by-name (개인키 호환) ──
            String puuid      = null;
            String summonerId = null;
            int    level      = 0;

            try {
                String url = String.format(
                        "https://%s.api.riotgames.com/tft/summoner/v1/summoners/by-name/%s",
                        platform, urlEncode(req.getGameName()));
                Map<String, Object> sd = requestMap(url);
                if (sd != null) {
                    puuid      = (String) sd.get("puuid");
                    summonerId = (String) sd.get("id");
                    level      = ((Number) sd.getOrDefault("summonerLevel", 0)).intValue();
                    log.info("TFT summoner by-name 성공 - puuid={}", puuid);
                }
            } catch (Exception e) {
                log.warn("TFT summoner by-name 실패, Account API로 재시도: {}", e.getMessage());
            }

            // ── STEP 2: fallback - Account API ──
            if (puuid == null && normalizeRegionToken(req.getTagLine()) == null) {
                String accountUrl = String.format(
                        "https://%s.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s",
                        routing, urlEncode(req.getGameName()), urlEncode(req.getTagLine()));
                log.info("TFT Account API URL: {}", accountUrl);
                Map<String, Object> accountData = requestMap(accountUrl);
                if (accountData == null) throw new RuntimeException("계정 정보를 찾을 수 없습니다.");
                puuid = (String) accountData.get("puuid");

                try {
                    Map<String, Object> sd = requestMap(String.format(
                            "https://%s.api.riotgames.com/tft/summoner/v1/summoners/by-puuid/%s",
                            platform, puuid));
                    if (sd != null) {
                        summonerId = (String) sd.get("id");
                        level      = ((Number) sd.getOrDefault("summonerLevel", 0)).intValue();
                    }
                } catch (Exception ex) {
                    log.warn("TFT summoner by-puuid 실패: {}", ex.getMessage());
                }
            }

            // ── STEP 3: TFT 랭크 ──
            if (puuid == null) {
                throw new RuntimeException("계정을 찾을 수 없습니다. 닉네임과 서버/태그 형식을 확인하세요.");
            }

            String tier = "UNRANKED", rank = "", lp = "0";
            if (summonerId != null) {
                try {
                    List<Map<String, Object>> rankData = requestListOfMap(String.format(
                            "https://%s.api.riotgames.com/tft/league/v1/entries/by-summoner/%s",
                            platform, summonerId));
                    if (rankData != null && !rankData.isEmpty()) {
                        Map<String, Object> entry = rankData.get(0);
                        tier = (String) entry.getOrDefault("tier", "UNRANKED");
                        rank = (String) entry.getOrDefault("rank", "");
                        lp   = String.valueOf(entry.getOrDefault("leaguePoints", 0));
                    }
                } catch (Exception e) {
                    log.warn("TFT 랭크 조회 실패: {}", e.getMessage());
                }
            }

            // ── STEP 4: TFT 매치 목록 ──
            int count = Math.min(req.getCount() != null ? req.getCount() : 20, 20);
            List<String> matchIds = null;
            if (puuid != null) {
                try {
                    matchIds = requestListOfString(String.format(
                            "https://%s.api.riotgames.com/tft/match/v1/matches/by-puuid/%s/ids?start=0&count=%d",
                            routing, puuid, count));
                } catch (Exception e) {
                    log.warn("TFT 매치 목록 조회 실패: {}", e.getMessage());
                }
            }

            // ── STEP 5: 매치 상세 ──
            List<MatchInfo> matches = new ArrayList<>();
            if (matchIds != null) {
                for (String matchId : matchIds) {
                    try {
                        MatchInfo info = fetchTftMatchDetail(matchId, puuid, routing);
                        if (info != null) matches.add(info);
                    } catch (Exception e) {
                        log.warn("TFT 매치 상세 실패 - {}: {}", matchId, e.getMessage());
                    }
                }
            }

            MatchStats stats = buildStats(matches);
            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(puuid != null ? puuid : "")
                    .gameName(req.getGameName()).tagLine(req.getTagLine())
                    .summonerLevel(String.valueOf(level))
                    .tier(tier).rank(rank).lp(lp + " LP").build();

            return PlayerSearchResponse.builder()
                    .success(true).game("tft").nickname(nickname)
                    .playerInfo(playerInfo).matches(matches).stats(stats).build();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("TFT Riot API key unauthorized: {}", e.getResponseBodyAsString());
                return PlayerSearchResponse.error("tft", nickname, "Riot API 키가 만료되었거나 올바르지 않습니다. 새 RIOT_API_KEY로 교체해 주세요.");
            }
            log.error("TFT 전적 검색 오류 - {}", nickname, e);
            return PlayerSearchResponse.error("tft", nickname, e.getMessage());
        } catch (Exception e) {
            log.error("TFT 전적 검색 오류 - {}", nickname, e);
            return PlayerSearchResponse.error("tft", nickname, e.getMessage());
        }
    }

    private MatchInfo fetchTftMatchDetail(String matchId, String puuid, String routing) {
        Map<String, Object> match = requestMap(String.format(
                "https://%s.api.riotgames.com/tft/match/v1/matches/%s", routing, matchId));
        if (match == null) return null;

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) match.get("info");
        if (info == null) return null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");
        if (participants == null) return null;

        Map<String, Object> me = participants.stream()
                .filter(p -> puuid.equals(p.get("puuid"))).findFirst().orElse(null);
        if (me == null) return null;

        int placement = toInt(me.get("placement"));
        boolean win   = placement <= 4;

        long ts = toLong(info.get("game_datetime"));
        String playedAt = ts > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("Asia/Seoul")).format(FORMATTER)
                : "";

        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("placement",         placement + "위");
        extras.put("level",             me.get("level"));
        extras.put("totalDamage",       me.get("total_damage_to_players"));
        extras.put("playersEliminated", me.get("players_eliminated"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> units = (List<Map<String, Object>>) me.get("units");
        if (units != null) {
            List<String> unitNames = units.stream()
                    .map(u -> (String) u.getOrDefault("character_id", ""))
                    .filter(s -> !s.isEmpty()).collect(Collectors.toList());
            extras.put("units", unitNames);
        }

        return MatchInfo.builder()
                .matchId(matchId).gameMode("TFT").win(win)
                .kills(toInt(me.get("players_eliminated"))).deaths(0).assists(0)
                .kda((double) placement)
                .playtime(toInt(info.get("game_length"))).playedAt(playedAt).extras(extras).build();
    }

    private MatchStats buildStats(List<MatchInfo> matches) {
        if (matches.isEmpty()) return MatchStats.builder().build();
        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        double avgPlacement = matches.stream()
                .mapToDouble(m -> m.getKda() != null ? m.getKda() : 4.5).average().orElse(4.5);
        return MatchStats.builder()
                .totalGames(matches.size()).wins(wins).losses(matches.size() - wins)
                .winRate(Math.round((double) wins / matches.size() * 1000.0) / 10.0)
                .avgKda(Math.round(avgPlacement * 10.0) / 10.0).build();
    }

    private String withKey(String url) {
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "api_key=" + riotApiKey.trim();
    }

    private Map<String, Object> requestMap(String url) {
        return restTemplate.exchange(withKey(url), HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
    }

    private List<Map<String, Object>> requestListOfMap(String url) {
        return restTemplate.exchange(withKey(url), HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }

    private List<String> requestListOfString(String url) {
        return restTemplate.exchange(withKey(url), HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {}).getBody();
    }

    private int toInt(Object o)   { return o instanceof Number n ? n.intValue()  : 0;  }
    private long toLong(Object o) { return o instanceof Number n ? n.longValue() : 0L; }
    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }

    private String resolveRegionHint(PlayerSearchRequest req) {
        String region = normalizeRegionToken(req.getRegion());
        if (region != null) return region;
        String tagRegion = normalizeRegionToken(req.getTagLine());
        if (tagRegion != null) return tagRegion;
        return "kr";
    }

    private String normalizeRegionToken(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase();
        return PLATFORM_NORMALIZE.containsKey(normalized) ? normalized : null;
    }
}
