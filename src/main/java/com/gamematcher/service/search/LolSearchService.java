package com.gamematcher.service.search;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.MatchInfo;
import com.gamematcher.dto.search.PlayerSearchResponse.MatchStats;
import com.gamematcher.dto.search.PlayerSearchResponse.PlayerInfo;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LolSearchService {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key:}")
    private String riotApiKey;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> PLATFORM = new HashMap<>();
    private static final Map<String, String> ROUTING = new HashMap<>();
    private static final Map<Integer, String> QUEUE_LABEL = new HashMap<>();

    static {
        PLATFORM.put("kr", "kr");
        PLATFORM.put("kr1", "kr");
        PLATFORM.put("jp", "jp1");
        PLATFORM.put("jp1", "jp1");
        PLATFORM.put("na", "na1");
        PLATFORM.put("na1", "na1");
        PLATFORM.put("euw", "euw1");
        PLATFORM.put("euw1", "euw1");
        PLATFORM.put("eune", "eune1");
        PLATFORM.put("eune1", "eune1");
        PLATFORM.put("br", "br1");
        PLATFORM.put("br1", "br1");
        PLATFORM.put("la1", "la1");
        PLATFORM.put("la2", "la2");
        PLATFORM.put("tr", "tr1");
        PLATFORM.put("tr1", "tr1");
        PLATFORM.put("ru", "ru");
        PLATFORM.put("oc1", "oc1");

        ROUTING.put("kr", "asia");
        ROUTING.put("jp1", "asia");
        ROUTING.put("na1", "americas");
        ROUTING.put("la1", "americas");
        ROUTING.put("la2", "americas");
        ROUTING.put("br1", "americas");
        ROUTING.put("euw1", "europe");
        ROUTING.put("eune1", "europe");
        ROUTING.put("tr1", "europe");
        ROUTING.put("ru", "europe");
        ROUTING.put("oc1", "sea");

        QUEUE_LABEL.put(420, "RANKED_SOLO_5x5");
        QUEUE_LABEL.put(440, "RANKED_FLEX_SR");
        QUEUE_LABEL.put(450, "ARAM");
        QUEUE_LABEL.put(400, "NORMAL_DRAFT");
        QUEUE_LABEL.put(430, "NORMAL_BLIND");
        QUEUE_LABEL.put(490, "QUICKPLAY");
        QUEUE_LABEL.put(700, "CLASH");
    }

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String tagLine = req.getTagLine() != null ? req.getTagLine().trim() : "";
        String nickname = req.getGameName() + (tagLine.isBlank() ? "" : "#" + tagLine);

        if (riotApiKey == null || riotApiKey.isBlank()) {
            return PlayerSearchResponse.error("lol", nickname, "Riot API 키가 설정되지 않았습니다.");
        }

        try {
            String regionHint = resolveRegionHint(req);
            String platform = PLATFORM.getOrDefault(regionHint, "kr");
            String routing = ROUTING.getOrDefault(platform, "asia");

            SummonerLookup lookup = fetchSummoner(req.getGameName(), tagLine, platform, routing);
            if (lookup == null || lookup.puuid == null) {
                return PlayerSearchResponse.error("lol", nickname, "계정을 찾을 수 없습니다. 닉네임/서버/태그 형식을 확인하세요.");
            }

            String puuid = lookup.puuid;
            platform = lookup.platform;
            routing = lookup.routing;

            String tier = "UNRANKED";
            String rank = "";
            String lp = "0";
            int rankedWins = 0;
            int rankedLosses = 0;

            try {
                List<Map<String, Object>> rankList = getListOfMap(
                        "https://" + platform + ".api.riotgames.com/lol/league/v4/entries/by-puuid/" + puuid);
                if (rankList != null) {
                    for (Map<String, Object> entry : rankList) {
                        if ("RANKED_SOLO_5x5".equals(entry.get("queueType"))) {
                            tier = String.valueOf(entry.getOrDefault("tier", "UNRANKED"));
                            rank = String.valueOf(entry.getOrDefault("rank", ""));
                            lp = String.valueOf(entry.getOrDefault("leaguePoints", 0));
                            rankedWins = toInt(entry.get("wins"));
                            rankedLosses = toInt(entry.get("losses"));
                            if (lookup.summonerId == null) {
                                lookup = lookup.toBuilder().summonerId((String) entry.get("summonerId")).build();
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("LoL rank lookup failed: {}", e.getMessage());
            }

            int count = Math.min(req.getCount() != null ? req.getCount() : 10, 20);
            Integer queueId = req.getQueueType() != null ? req.getQueueType() : 420;
            List<String> matchIds = fetchMatchIds(routing, puuid, queueId, count);
            if (matchIds.isEmpty() && queueId == 420) {
                matchIds = getListOfString(String.format(
                        "https://%s.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=%d",
                        routing, puuid, count));
                if (matchIds == null) matchIds = new ArrayList<>();
            }

            List<MatchInfo> matches = new ArrayList<>();
            for (String matchId : matchIds) {
                try {
                    MatchInfo info = fetchMatchDetail(matchId, puuid, routing);
                    if (info != null) matches.add(info);
                } catch (Exception e) {
                    log.warn("LoL match detail failed - {}: {}", matchId, e.getMessage());
                }
            }

            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("summonerId", lookup.summonerId);
            rawData.put("rankedWins", rankedWins);
            rawData.put("rankedLosses", rankedLosses);
            if (rankedWins + rankedLosses > 0) {
                rawData.put("rankedWinRate",
                        Math.round((double) rankedWins / (rankedWins + rankedLosses) * 1000.0) / 10.0 + "%");
            }

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(puuid)
                    .gameName(req.getGameName())
                    .tagLine(tagLine)
                    .summonerLevel(String.valueOf(lookup.level))
                    .profileIconId(String.valueOf(lookup.iconId))
                    .tier(tier)
                    .rank(rank)
                    .lp(lp + " LP")
                    .rawData(rawData)
                    .build();

            return PlayerSearchResponse.builder()
                    .success(true)
                    .game("lol")
                    .nickname(nickname)
                    .playerInfo(playerInfo)
                    .matches(matches)
                    .stats(buildStats(matches))
                    .build();
        } catch (Exception e) {
            log.error("LoL search failed - {}: {}", nickname, e.getMessage(), e);
            return PlayerSearchResponse.error("lol", nickname, e.getMessage());
        }
    }

    private SummonerLookup fetchSummoner(String gameName, String tagLine, String platform, String routing) {
        SummonerLookup byName = fetchSummonerByName(platform, routing, gameName);
        if (byName != null) {
            return byName;
        }
        if (tagLine == null || tagLine.isBlank()) {
            return null;
        }
        for (String candidateRouting : resolveCandidateRoutings(routing)) {
            try {
                String puuid = fetchPuuid(candidateRouting, gameName, tagLine);
                if (puuid == null) continue;
                Map<String, Object> summoner = getMap(
                        "https://" + platform + ".api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + puuid);
                return SummonerLookup.builder()
                        .puuid(puuid)
                        .summonerId(summoner != null ? (String) summoner.get("id") : null)
                        .level(summoner != null ? toInt(summoner.get("summonerLevel")) : 0)
                        .iconId(summoner != null ? summoner.getOrDefault("profileIconId", 0) : 0)
                        .platform(platform)
                        .routing(candidateRouting)
                        .build();
            } catch (Exception e) {
                log.warn("LoL account lookup failed (routing={}): {}", candidateRouting, e.getMessage());
            }
        }
        return null;
    }

    private SummonerLookup fetchSummonerByName(String platform, String routing, String gameName) {
        try {
            Map<String, Object> summoner = getMap(
                    "https://" + platform + ".api.riotgames.com/lol/summoner/v4/summoners/by-name/" + urlEncode(gameName));
            if (summoner == null || summoner.get("puuid") == null) return null;
            return SummonerLookup.builder()
                    .puuid((String) summoner.get("puuid"))
                    .summonerId((String) summoner.get("id"))
                    .level(toInt(summoner.get("summonerLevel")))
                    .iconId(summoner.getOrDefault("profileIconId", 0))
                    .platform(platform)
                    .routing(routing)
                    .build();
        } catch (Exception e) {
            log.warn("LoL summoner-by-name failed: {}", e.getMessage());
            return null;
        }
    }

    private String fetchPuuid(String routing, String gameName, String tagLine) {
        try {
            String url = String.format(
                    "https://%s.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s",
                    routing, urlEncode(gameName), urlEncode(tagLine));
            Map<String, Object> data = getMap(url);
            return data != null ? (String) data.get("puuid") : null;
        } catch (HttpClientErrorException e) {
            log.warn("LoL riot account lookup failed ({}): {}", routing, e.getResponseBodyAsString());
            return null;
        }
    }

    private List<String> fetchMatchIds(String routing, String puuid, int queueId, int count) {
        List<String> result = new ArrayList<>();
        try {
            if (queueId == -1) {
                int perQueue = Math.max(count / 2, 3);
                for (int queue : new int[]{400, 430, 490}) {
                    List<String> ids = getListOfString(String.format(
                            "https://%s.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?queue=%d&start=0&count=%d",
                            routing, puuid, queue, perQueue));
                    if (ids != null) result.addAll(ids);
                }
                return result.stream().distinct().limit(count).collect(Collectors.toList());
            }
            List<String> ids = getListOfString(String.format(
                    "https://%s.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?queue=%d&start=0&count=%d",
                    routing, puuid, queueId, count));
            if (ids != null) result.addAll(ids);
        } catch (Exception e) {
            log.warn("LoL match id lookup failed: {}", e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private MatchInfo fetchMatchDetail(String matchId, String puuid, String routing) {
        Map<String, Object> match = getMap(
                "https://" + routing + ".api.riotgames.com/lol/match/v5/matches/" + matchId);
        if (match == null) return null;

        Map<String, Object> info = (Map<String, Object>) match.get("info");
        if (info == null) return null;

        List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");
        if (participants == null) return null;

        Map<String, Object> me = participants.stream()
                .filter(p -> puuid.equals(p.get("puuid")))
                .findFirst()
                .orElse(null);
        if (me == null) return null;

        int kills = toInt(me.get("kills"));
        int deaths = toInt(me.get("deaths"));
        int assists = toInt(me.get("assists"));
        double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;

        long gameEnd = toLong(info.get("gameEndTimestamp"));
        String playedAt = gameEnd > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(gameEnd), ZoneId.of("Asia/Seoul")).format(FORMATTER)
                : "";

        int queueId = toInt(info.get("queueId"));
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("queueId", queueId);
        extras.put("goldEarned", me.get("goldEarned"));
        extras.put("totalDamage", me.get("totalDamageDealtToChampions"));
        extras.put("wardPlaced", me.get("wardsPlaced"));
        extras.put("visionScore", me.get("visionScore"));

        return MatchInfo.builder()
                .matchId(matchId)
                .gameMode(QUEUE_LABEL.getOrDefault(queueId, "CLASSIC"))
                .champion((String) me.getOrDefault("championName", ""))
                .win((Boolean) me.getOrDefault("win", false))
                .kills(kills)
                .deaths(deaths)
                .assists(assists)
                .kda(Math.round(kda * 100.0) / 100.0)
                .cs(toInt(me.get("totalMinionsKilled")) + toInt(me.get("neutralMinionsKilled")))
                .playtime(toInt(info.get("gameDuration")))
                .playedAt(playedAt)
                .extras(extras)
                .build();
    }

    private MatchStats buildStats(List<MatchInfo> matches) {
        if (matches.isEmpty()) return MatchStats.builder().build();
        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        double avgK = matches.stream().mapToInt(m -> m.getKills() != null ? m.getKills() : 0).average().orElse(0);
        double avgD = matches.stream().mapToInt(m -> m.getDeaths() != null ? m.getDeaths() : 0).average().orElse(0);
        double avgA = matches.stream().mapToInt(m -> m.getAssists() != null ? m.getAssists() : 0).average().orElse(0);
        double avgKda = matches.stream().mapToDouble(m -> m.getKda() != null ? m.getKda() : 0).average().orElse(0);
        String most = matches.stream()
                .filter(m -> m.getChampion() != null && !m.getChampion().isBlank())
                .collect(Collectors.groupingBy(MatchInfo::getChampion, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
        return MatchStats.builder()
                .totalGames(matches.size())
                .wins(wins)
                .losses(matches.size() - wins)
                .winRate(Math.round((double) wins / matches.size() * 1000.0) / 10.0)
                .avgKills(Math.round(avgK * 10.0) / 10.0)
                .avgDeaths(Math.round(avgD * 10.0) / 10.0)
                .avgAssists(Math.round(avgA * 10.0) / 10.0)
                .avgKda(Math.round(avgKda * 100.0) / 100.0)
                .mostUsedChampionOrAgent(most)
                .build();
    }

    private String resolveRegionHint(PlayerSearchRequest req) {
        String tagRegion = normalizeRegionToken(req.getTagLine());
        if (tagRegion != null) return tagRegion;
        String region = normalizeRegionToken(req.getRegion());
        if (region != null) return region;
        return "kr";
    }

    private String normalizeRegionToken(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase();
        return PLATFORM.containsKey(normalized) ? normalized : null;
    }

    private List<String> resolveCandidateRoutings(String primaryRouting) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (primaryRouting != null && !primaryRouting.isBlank()) candidates.add(primaryRouting);
        candidates.add("asia");
        candidates.add("americas");
        candidates.add("europe");
        candidates.add("sea");
        return new ArrayList<>(candidates);
    }

    private String key(String url) {
        return url;
    }

    private HttpEntity<Void> riotRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey.trim());
        return new HttpEntity<>(headers);
    }

    private Map<String, Object> getMap(String url) {
        return restTemplate.exchange(URI.create(key(url)), HttpMethod.GET, riotRequestEntity(),
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
    }

    private List<Map<String, Object>> getListOfMap(String url) {
        return restTemplate.exchange(URI.create(key(url)), HttpMethod.GET, riotRequestEntity(),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }

    private List<String> getListOfString(String url) {
        return restTemplate.exchange(URI.create(key(url)), HttpMethod.GET, riotRequestEntity(),
                new ParameterizedTypeReference<List<String>>() {}).getBody();
    }

    private int toInt(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private long toLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Builder(toBuilder = true)
    private static class SummonerLookup {
        private String puuid;
        private String summonerId;
        private int level;
        private Object iconId;
        private String platform;
        private String routing;
    }
}
