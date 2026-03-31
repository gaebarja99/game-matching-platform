package com.gamematcher.service.search;

import com.gamematcher.dto.valorant.ValorantLifetimeApiResponse;
import com.gamematcher.dto.valorant.ValorantLifetimeDataItem;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.MatchInfo;
import com.gamematcher.dto.search.PlayerSearchResponse.MatchStats;
import com.gamematcher.dto.search.PlayerSearchResponse.PlayerInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValorantSearchService {

    private static final DateTimeFormatter LIFETIME_PLAYED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestTemplate restTemplate;

    @Value("${valorant.api.key:}")
    private String henrikApiKey;

    private static final Map<String, String> REGION_MAP = new HashMap<>();

    static {
        REGION_MAP.put("kr", "kr");
        REGION_MAP.put("kr1", "kr");
        REGION_MAP.put("ap", "ap");
        REGION_MAP.put("asia", "ap");
        REGION_MAP.put("jp", "ap");
        REGION_MAP.put("jp1", "ap");
        REGION_MAP.put("sg", "ap");
        REGION_MAP.put("na", "na");
        REGION_MAP.put("na1", "na");
        REGION_MAP.put("eu", "eu");
        REGION_MAP.put("euw", "eu");
        REGION_MAP.put("euw1", "eu");
        REGION_MAP.put("eune1", "eu");
        REGION_MAP.put("latam", "latam");
        REGION_MAP.put("la1", "latam");
        REGION_MAP.put("la2", "latam");
        REGION_MAP.put("br", "br");
        REGION_MAP.put("br1", "br");
    }

    /** Henrik v1/v3/lifetime 엔드포인트에 쓰는 리전 코드 */
    private static final Set<String> HENRIK_SHARDS = Set.of("kr", "ap", "na", "eu", "latam", "br");

    /**
     * 계정 API의 region 문자열 → Henrik 샤드. 알 수 없으면 null.
     */
    private static String toHenrikShard(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.toLowerCase().trim();
        if (REGION_MAP.containsKey(t)) {
            return REGION_MAP.get(t);
        }
        if (HENRIK_SHARDS.contains(t)) {
            return t;
        }
        return null;
    }

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String nickname = req.getGameName() + "#" + req.getTagLine();
        String inputRegion = req.getRegion() != null ? req.getRegion().toLowerCase().trim() : "kr";

        log.info("Valorant search - gameName={}, tagLine={}, region(input)={}, accountOnly={}, prefetchPuuid={}",
                req.getGameName(), req.getTagLine(), inputRegion,
                Boolean.TRUE.equals(req.getAccountOnly()),
                req.getValorantPrefetchPuuid() != null && !req.getValorantPrefetchPuuid().isBlank());

        try {
            HttpEntity<Void> entity = createEntity();

            if (Boolean.TRUE.equals(req.getAccountOnly())) {
                Map<String, Object> probeAccount = fetchAccountData(req, entity);
                if (probeAccount == null) {
                    throw new RuntimeException("발로란트 계정을 찾을 수 없습니다.");
                }
                String probePuuid = asString(probeAccount.get("puuid"));
                String probeCard = extractCardUrl(probeAccount);
                String probeAccountRegion = asString(probeAccount.get("region"));
                List<String> probeShards = resolveCandidateRegions(inputRegion, probeAccountRegion);
                String shardForMmr = probeShards.isEmpty() ? "kr" : probeShards.get(0);
                Map<String, Object> rawData = new LinkedHashMap<>();
                rawData.put("valorantRegion", shardForMmr);
                PlayerInfo probeInfo = PlayerInfo.builder()
                        .puuid(probePuuid)
                        .gameName(req.getGameName())
                        .tagLine(req.getTagLine())
                        .tier("")
                        .rank(shardForMmr)
                        .avatarUrl(probeCard)
                        .rawData(rawData)
                        .build();
                return PlayerSearchResponse.builder()
                        .success(true)
                        .game("valorant")
                        .nickname(nickname)
                        .valorantMmrPending(true)
                        .playerInfo(probeInfo)
                        .matches(List.of())
                        .stats(buildStats(List.of()))
                        .build();
            }

            String puuid;
            String cardUrl;
            String accountRegion;
            boolean usePrefetch = req.getValorantPrefetchPuuid() != null && !req.getValorantPrefetchPuuid().isBlank()
                    && !Boolean.TRUE.equals(req.getForceRefresh());
            if (usePrefetch) {
                puuid = req.getValorantPrefetchPuuid().trim();
                cardUrl = req.getValorantPrefetchCardUrl();
                accountRegion = req.getValorantPrefetchAccountRegion();
            } else {
                Map<String, Object> accountData = fetchAccountData(req, entity);
                if (accountData == null) {
                    throw new RuntimeException("발로란트 계정을 찾을 수 없습니다.");
                }
                puuid = asString(accountData.get("puuid"));
                cardUrl = extractCardUrl(accountData);
                accountRegion = asString(accountData.get("region"));
            }

            List<String> candidateRegions = resolveCandidateRegions(inputRegion, accountRegion);

            boolean deferMmr = Boolean.TRUE.equals(req.getDeferValorantMmr());
            String finalRegion = candidateRegions.get(0);
            String tier = "UNRANKED";
            String tierName = "";
            if (!deferMmr) {
                for (String candidateRegion : candidateRegions) {
                    try {
                        String mmrUrl = UriComponentsBuilder
                                .fromHttpUrl("https://api.henrikdev.xyz")
                                .path("/valorant/v2/by-puuid/mmr/{region}/{puuid}")
                                .buildAndExpand(candidateRegion, puuid)
                                .toUriString();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mmrResp = restTemplate.exchange(mmrUrl, HttpMethod.GET, entity, Map.class).getBody();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mmrData = mmrResp != null ? (Map<String, Object>) mmrResp.get("data") : null;
                        if (mmrData == null) {
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> current = (Map<String, Object>) mmrData.get("current_data");
                        if (current == null) {
                            continue;
                        }
                        tier = String.valueOf(current.getOrDefault("currenttier", 0));
                        tierName = asString(current.getOrDefault("currenttierpatched", "UNRANKED"));
                        finalRegion = candidateRegion;
                        break;
                    } catch (Exception e) {
                        log.warn("Valorant MMR lookup failed (region={}): {}", candidateRegion, e.getMessage());
                    }
                }
            }

            int count = Math.min(req.getCount() != null ? req.getCount() : 10, 20);
            List<Map<String, Object>> matchData = List.of();
            List<MatchInfo> matches = List.of();
            for (String candidateRegion : candidateRegions) {
                String matchUrl = UriComponentsBuilder
                        .fromHttpUrl("https://api.henrikdev.xyz")
                        .path("/valorant/v3/by-puuid/matches/{region}/{puuid}")
                        .queryParam("size", count)
                        .buildAndExpand(candidateRegion, puuid)
                        .toUriString();
                log.info("Valorant matchUrl={}", matchUrl);
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> matchResp = restTemplate.exchange(matchUrl, HttpMethod.GET, entity, Map.class).getBody();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> data = matchResp != null
                            ? (List<Map<String, Object>>) matchResp.get("data")
                            : List.of();
                    if (data != null && !data.isEmpty()) {
                        matchData = data;
                        matches = parseMatches(matchData, puuid);
                        finalRegion = candidateRegion;
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Valorant match lookup failed (region={}): {}", candidateRegion, e.getMessage());
                }
            }
            if (matches.size() < count) {
                for (String candidateRegion : candidateRegions) {
                    List<MatchInfo> lifetimeMatches = fetchMatchesFromLifetime(candidateRegion, puuid, count, entity);
                    if (!lifetimeMatches.isEmpty()) {
                        matches = mergeMatches(matches, lifetimeMatches, count);
                        finalRegion = candidateRegion;
                        if (matches.size() >= count) {
                            break;
                        }
                    }
                }
            }
            if (matches.size() < count) {
                for (String candidateRegion : candidateRegions) {
                    List<MatchInfo> storedMatches = fetchMatchesFromStoredMatches(
                            candidateRegion, req.getGameName(), req.getTagLine(), count, entity);
                    if (!storedMatches.isEmpty()) {
                        matches = mergeMatches(matches, storedMatches, count);
                        finalRegion = candidateRegion;
                        if (matches.size() >= count) {
                            break;
                        }
                    }
                }
            }
            MatchStats stats = buildStats(matches);

            Map<String, Object> rawData = null;
            if (deferMmr) {
                rawData = new LinkedHashMap<>();
                rawData.put("valorantRegion", finalRegion);
            }
            PlayerInfo.PlayerInfoBuilder pi = PlayerInfo.builder()
                    .puuid(puuid)
                    .gameName(req.getGameName())
                    .tagLine(req.getTagLine())
                    .tier(deferMmr ? "" : (tierName == null || tierName.isBlank() ? tier : tierName))
                    .rank(finalRegion)
                    .avatarUrl(cardUrl);
            if (rawData != null) {
                pi.rawData(rawData);
            }
            PlayerInfo playerInfo = pi.build();

            return PlayerSearchResponse.builder()
                    .success(true)
                    .game("valorant")
                    .nickname(nickname)
                    .valorantMmrPending(deferMmr ? Boolean.TRUE : null)
                    .playerInfo(playerInfo)
                    .matches(matches)
                    .stats(stats)
                    .build();
        } catch (Exception e) {
            log.error("Valorant search failed - {}", nickname, e);
            return PlayerSearchResponse.error("valorant", nickname, e.getMessage());
        }
    }

    private HttpEntity<Void> createEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (henrikApiKey != null && !henrikApiKey.isBlank()) {
            headers.set("Authorization", henrikApiKey.trim());
        }
        return new HttpEntity<>(headers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAccountData(PlayerSearchRequest req, HttpEntity<Void> entity) {
        boolean bustCache = Boolean.TRUE.equals(req.getForceRefresh());
        String forceQ = bustCache ? "?force=true" : "";
        List<String> candidateUrls = List.of(
                String.format("https://api.henrikdev.xyz/valorant/v2/account/%s/%s%s",
                        req.getGameName(), req.getTagLine(), forceQ),
                String.format("https://api.henrikdev.xyz/valorant/v1/account/%s/%s%s",
                        req.getGameName(), req.getTagLine(), forceQ)
        );

        RuntimeException lastError = null;
        for (String accountUrl : candidateUrls) {
            log.info("Valorant accountUrl={}", accountUrl);
            try {
                Map<String, Object> accountResp = restTemplate.exchange(
                        accountUrl, HttpMethod.GET, entity, Map.class).getBody();
                Map<String, Object> accountData = accountResp != null
                        ? (Map<String, Object>) accountResp.get("data")
                        : null;
                if (accountData != null && accountData.get("puuid") != null) {
                    return accountData;
                }
            } catch (HttpStatusCodeException e) {
                lastError = new RuntimeException(
                        "Valorant account lookup failed: " + e.getStatusCode() + " / " + e.getResponseBodyAsString(), e);
            } catch (Exception e) {
                lastError = new RuntimeException("Valorant account lookup failed: " + e.getMessage(), e);
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        return null;
    }

    /**
     * 계정 조회 응답에 region이 오면 puuid 기준 API는 그 샤드 하나만 호출하면 됨.
     * (다른 샤드에 같은 puuid로 조회하면 Account not found 404만 연쇄로 남.)
     * region이 비었거나 매핑 불가할 때만 요청 region + 공통 샤드 순으로 폴백.
     */
    private List<String> resolveCandidateRegions(String inputRegion, String accountRegion) {
        String fromAccount = toHenrikShard(accountRegion);
        if (fromAccount != null) {
            return List.of(fromAccount);
        }
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String fromInput = toHenrikShard(inputRegion);
        if (fromInput != null) {
            candidates.add(fromInput);
        }
        candidates.add("kr");
        candidates.add("ap");
        candidates.add("na");
        candidates.add("eu");
        candidates.add("latam");
        candidates.add("br");
        return new ArrayList<>(candidates);
    }

    private List<MatchInfo> fetchMatchesFromLifetime(String region, String puuid, int count, HttpEntity<Void> entity) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.henrikdev.xyz")
                .path("/valorant/v1/lifetime/matches/{region}/by-puuid/{puuid}")
                .queryParam("size", Math.min(Math.max(count, 1), 100))
                .buildAndExpand(region, puuid)
                .toUriString();
        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                return List.of();
            }
            ValorantLifetimeApiResponse parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.getBody(), ValorantLifetimeApiResponse.class);
            if (parsed.getData() == null || parsed.getData().isEmpty()) {
                return List.of();
            }
            return parsed.getData().stream()
                    .map(this::matchInfoFromLifetimeItem)
                    .filter(Objects::nonNull)
                    .limit(count)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Valorant lifetime match lookup failed (region={}): {}", region, e.getMessage());
            return List.of();
        }
    }

    private List<MatchInfo> fetchMatchesFromStoredMatches(String region, String gameName, String tagLine, int count, HttpEntity<Void> entity) {
        String url = String.format(
                "https://api.henrikdev.xyz/valorant/v1/stored-matches/%s/%s/%s?size=%d",
                region, gameName, tagLine, Math.min(Math.max(count, 1), 100)
        );
        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                return List.of();
            }
            ValorantLifetimeApiResponse parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.getBody(), ValorantLifetimeApiResponse.class);
            if (parsed.getData() == null || parsed.getData().isEmpty()) {
                return List.of();
            }
            return parsed.getData().stream()
                    .map(this::matchInfoFromLifetimeItem)
                    .filter(Objects::nonNull)
                    .limit(count)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Valorant stored-matches lookup failed (region={}): {}", region, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<MatchInfo> parseMatches(List<Map<String, Object>> matchData, String puuid) {
        if (matchData == null) {
            return List.of();
        }

        List<MatchInfo> result = new ArrayList<>();
        for (Map<String, Object> match : matchData) {
            try {
                Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");
                Map<String, Object> players = (Map<String, Object>) match.get("players");
                Map<String, Object> teams = (Map<String, Object>) match.get("teams");
                if (metadata == null || players == null) {
                    continue;
                }

                List<Map<String, Object>> allPlayers = (List<Map<String, Object>>) players.get("all_players");
                if (allPlayers == null) {
                    continue;
                }

                Map<String, Object> me = allPlayers.stream()
                        .filter(p -> puuid.equals(p.get("puuid")))
                        .findFirst()
                        .orElse(null);
                if (me == null) {
                    continue;
                }

                Map<String, Object> stats = (Map<String, Object>) me.get("stats");
                int kills = stats != null ? toInt(stats.get("kills")) : 0;
                int deaths = stats != null ? toInt(stats.get("deaths")) : 0;
                int assists = stats != null ? toInt(stats.get("assists")) : 0;
                double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;

                String myTeam = asString(me.get("team"));
                boolean win = false;
                if (teams != null && myTeam != null) {
                    Map<String, Object> myTeamData = (Map<String, Object>) teams.get(myTeam.toLowerCase());
                    if (myTeamData != null) {
                        win = Boolean.TRUE.equals(myTeamData.get("has_won"));
                    }
                }

                Map<String, Object> extras = new LinkedHashMap<>();
                if (stats != null) {
                    extras.put("score", stats.get("score"));
                    extras.put("headshots", stats.get("headshots"));
                    extras.put("bodyshots", stats.get("bodyshots"));
                }

                result.add(MatchInfo.builder()
                        .matchId(asString(metadata.get("matchid")))
                        .gameMode(asString(metadata.getOrDefault("mode", "")))
                        .agent(asString(me.getOrDefault("character", "")))
                        .win(win)
                        .kills(kills)
                        .deaths(deaths)
                        .assists(assists)
                        .kda(Math.round(kda * 100.0) / 100.0)
                        .playedAt(asString(metadata.getOrDefault("game_start_patched", "")))
                        .extras(extras)
                        .build());
            } catch (Exception e) {
                log.warn("Valorant match parse failed", e);
            }
        }
        return result;
    }

    private MatchStats buildStats(List<MatchInfo> matches) {
        if (matches.isEmpty()) {
            return MatchStats.builder().build();
        }

        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        double avgK = matches.stream().mapToInt(m -> m.getKills() != null ? m.getKills() : 0).average().orElse(0);
        double avgD = matches.stream().mapToInt(m -> m.getDeaths() != null ? m.getDeaths() : 0).average().orElse(0);
        double avgA = matches.stream().mapToInt(m -> m.getAssists() != null ? m.getAssists() : 0).average().orElse(0);
        double avgKda = matches.stream().mapToDouble(m -> m.getKda() != null ? m.getKda() : 0).average().orElse(0);
        String mostAgent = matches.stream().filter(m -> m.getAgent() != null)
                .collect(Collectors.groupingBy(MatchInfo::getAgent, Collectors.counting()))
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
                .mostUsedChampionOrAgent(mostAgent)
                .build();
    }

    @SuppressWarnings("unchecked")
    private String extractCardUrl(Map<String, Object> data) {
        try {
            Object cardValue = data.get("card");
            if (cardValue instanceof Map<?, ?> card) {
                Object small = card.get("small");
                return small != null ? String.valueOf(small) : null;
            }
            Object playerCardValue = data.get("player_card");
            if (playerCardValue instanceof Map<?, ?> card) {
                Object small = card.get("small");
                return small != null ? String.valueOf(small) : null;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<MatchInfo> mergeMatches(List<MatchInfo> base, List<MatchInfo> extras, int limit) {
        LinkedHashMap<String, MatchInfo> merged = new LinkedHashMap<>();
        for (MatchInfo match : base) {
            if (match != null && match.getMatchId() != null) {
                merged.put(match.getMatchId(), match);
            }
        }
        for (MatchInfo match : extras) {
            if (match != null && match.getMatchId() != null && !merged.containsKey(match.getMatchId())) {
                merged.put(match.getMatchId(), match);
            }
            if (merged.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(merged.values());
    }

    private MatchInfo matchInfoFromLifetimeItem(ValorantLifetimeDataItem item) {
        if (item.getMeta() == null || item.getStats() == null) {
            return null;
        }
        ValorantLifetimeDataItem.ValorantLifetimeMeta meta = item.getMeta();
        ValorantLifetimeDataItem.ValorantLifetimeStats stats = item.getStats();
        String matchId = meta.getId();
        if (matchId == null || matchId.isBlank()) {
            return null;
        }

        int kills = stats.getKills();
        int deaths = stats.getDeaths();
        int assists = stats.getAssists();
        double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;

        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("score", stats.getScore());
        ValorantLifetimeDataItem.Shots shots = stats.getShots();
        extras.put("headshots", shots != null ? shots.getHead() : 0);
        extras.put("bodyshots", shots != null ? shots.getBody() : 0);
        if (meta.getMap() != null && meta.getMap().getName() != null) {
            extras.put("map", meta.getMap().getName());
        }

        return MatchInfo.builder()
                .matchId(matchId)
                .gameMode(meta.getMode() != null ? meta.getMode() : "")
                .agent(stats.getCharacter() != null ? stats.getCharacter().getName() : "")
                .win(inferLifetimeWin(stats.getTeam(), item.getTeams()))
                .kills(kills)
                .deaths(deaths)
                .assists(assists)
                .kda(Math.round(kda * 100.0) / 100.0)
                .playedAt(formatLifetimeStartedAt(meta.getStartedAt()))
                .extras(extras)
                .build();
    }

    private boolean inferLifetimeWin(String team, ValorantLifetimeDataItem.ValorantLifetimeTeams teams) {
        if (team == null || teams == null) {
            return false;
        }
        String normalizedTeam = team.trim().toLowerCase();
        if ("red".equals(normalizedTeam)) {
            return teams.getRed() > teams.getBlue();
        }
        if ("blue".equals(normalizedTeam)) {
            return teams.getBlue() > teams.getRed();
        }
        return false;
    }

    private String formatLifetimeStartedAt(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.of("Asia/Seoul")).format(LIFETIME_PLAYED_AT);
        } catch (Exception e) {
            return iso;
        }
    }
}
