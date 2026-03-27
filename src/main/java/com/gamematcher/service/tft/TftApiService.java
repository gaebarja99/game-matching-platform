package com.gamematcher.service.tft;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.RiotApiProperties;
import com.gamematcher.service.MatchApiCachePolicy;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import com.gamematcher.entity.match.TftMatch;
import com.gamematcher.repository.match.TftMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TFT 전적 조회 및 DB 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TftApiService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<String, String> PLATFORM_NORMALIZE = new HashMap<>();
    private static final Map<String, String> REGION_ROUTING = new HashMap<>();
    static {
        PLATFORM_NORMALIZE.put("kr", "kr"); PLATFORM_NORMALIZE.put("kr1", "kr");
        PLATFORM_NORMALIZE.put("jp", "jp1"); PLATFORM_NORMALIZE.put("jp1", "jp1");
        PLATFORM_NORMALIZE.put("na", "na1"); PLATFORM_NORMALIZE.put("na1", "na1");
        PLATFORM_NORMALIZE.put("euw", "euw1"); PLATFORM_NORMALIZE.put("euw1", "euw1");
        PLATFORM_NORMALIZE.put("br", "br1"); PLATFORM_NORMALIZE.put("br1", "br1");
        PLATFORM_NORMALIZE.put("la1", "la1"); PLATFORM_NORMALIZE.put("la2", "la2");
        REGION_ROUTING.put("kr", "asia"); REGION_ROUTING.put("jp1", "asia");
        REGION_ROUTING.put("na1", "americas"); REGION_ROUTING.put("la1", "americas");
        REGION_ROUTING.put("la2", "americas"); REGION_ROUTING.put("br1", "americas");
        REGION_ROUTING.put("euw1", "europe"); REGION_ROUTING.put("eune1", "europe");
        REGION_ROUTING.put("tr1", "europe"); REGION_ROUTING.put("ru", "europe");
    }

    private final RiotApiProperties riotApiProperties;
    private final TftMatchRepository tftMatchRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> getTftMatchIdsByPuuid(String puuid, int start, int count) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getRegionalBaseUrl())
                .path("/tft/match/v1/matches/by-puuid/{puuid}/ids")
                .queryParam("start", start)
                .queryParam("count", count)
                .buildAndExpand(puuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiProperties.getApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("TFT MatchIds API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        }
    }

    public void syncTftRecentMatches(String puuid, int count) {
        List<String> matchIds = getTftMatchIdsByPuuid(puuid, 0, count);
        if (matchIds == null || matchIds.isEmpty()) return;

        for (String matchId : matchIds) {
            if (tftMatchRepository.existsByPuuidAndMatchId(puuid, matchId)) continue;

            String url = UriComponentsBuilder
                    .fromHttpUrl(riotApiProperties.getRegionalBaseUrl())
                    .path("/tft/match/v1/matches/{matchId}")
                    .buildAndExpand(matchId)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", riotApiProperties.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode metadata = root.get("metadata");
                JsonNode info = root.get("info");
                JsonNode participants = info.get("participants");

                for (JsonNode p : participants) {
                    if (puuid.equals(p.get("puuid").asText())) {
                        TftMatch match = new TftMatch();
                        match.setPuuid(puuid);
                        match.setMatchId(metadata.get("match_id").asText());
                        match.setPlacement(p.get("placement").asInt());
                        match.setLevel(p.has("level") ? p.get("level").asInt() : 0);
                        match.setTotalPlayers(participants.size());
                        match.setTraits(p.has("traits") ? p.get("traits").toString() : null);
                        match.setUnits(p.has("units") ? p.get("units").toString() : null);
                        match.setGameDuration(info.has("game_datetime") ? (int) (info.get("game_datetime").asLong() / 1000) : null);
                        match.setGameCreation(info.has("game_datetime") ? info.get("game_datetime").asLong() : null);
                        match.setApiCachedAt(LocalDateTime.now());
                        tftMatchRepository.save(match);
                        break;
                    }
                }
            } catch (HttpStatusCodeException e) {
                throw new RuntimeException("TFT Match API 호출 실패: " + e.getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException("TFT Match 파싱 오류: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 전적 검색 (PlayerSearchResponse 반환)
     */
    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String nickname = req.getGameName() + "#" + req.getTagLine();
        if (riotApiProperties.getApiKey() == null || riotApiProperties.getApiKey().isBlank()) {
            return PlayerSearchResponse.error("tft", nickname, "Riot API 키가 설정되지 않았습니다.");
        }
        try {
            String rawRegion = req.getRegion() != null ? req.getRegion().toLowerCase() : "kr";
            String platform = PLATFORM_NORMALIZE.getOrDefault(rawRegion, rawRegion);
            String routing = REGION_ROUTING.getOrDefault(platform, "asia");
            log.info("TFT 검색 - gameName={}, tagLine={}, platform={}, routing={}",
                    req.getGameName(), req.getTagLine(), platform, routing);

            String puuid = null;
            String summonerId = null;
            int level = 0;
            try {
                String url = String.format("https://%s.api.riotgames.com/tft/summoner/v1/summoners/by-name/%s",
                        platform, urlEncode(req.getGameName()));
                Map<String, Object> sd = reqMap(url);
                if (sd != null) {
                    puuid = (String) sd.get("puuid");
                    summonerId = (String) sd.get("id");
                    level = ((Number) sd.getOrDefault("summonerLevel", 0)).intValue();
                }
            } catch (Exception e) {
                log.warn("TFT summoner by-name 실패, Account API로 재시도: {}", e.getMessage());
            }
            if (puuid == null) {
                String accountUrl = String.format("https://%s.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s",
                        routing, req.getGameName(), req.getTagLine());
                Map<String, Object> accountData = reqMap(accountUrl);
                if (accountData == null) throw new RuntimeException("계정 정보를 찾을 수 없습니다.");
                puuid = (String) accountData.get("puuid");
                try {
                    Map<String, Object> sd = reqMap(String.format("https://%s.api.riotgames.com/tft/summoner/v1/summoners/by-puuid/%s", platform, puuid));
                    if (sd != null) {
                        summonerId = (String) sd.get("id");
                        level = ((Number) sd.getOrDefault("summonerLevel", 0)).intValue();
                    }
                } catch (Exception ex) {
                    log.warn("TFT summoner by-puuid 실패: {}", ex.getMessage());
                }
            }

            String tier = "UNRANKED", rank = "", lp = "0";
            if (summonerId != null) {
                try {
                    List<Map<String, Object>> rankData = reqListOfMap(String.format(
                            "https://%s.api.riotgames.com/tft/league/v1/entries/by-summoner/%s", platform, summonerId));
                    if (rankData != null && !rankData.isEmpty()) {
                        Map<String, Object> entry = rankData.get(0);
                        tier = (String) entry.getOrDefault("tier", "UNRANKED");
                        rank = (String) entry.getOrDefault("rank", "");
                        lp = String.valueOf(entry.getOrDefault("leaguePoints", 0));
                    }
                } catch (Exception e) {
                    log.warn("TFT 랭크 조회 실패: {}", e.getMessage());
                }
            }

            int count = Math.min(req.getCount() != null ? req.getCount() : 5, 20);
            List<String> matchIds = null;
            if (puuid != null) {
                try {
                    matchIds = reqListOfString(String.format(
                            "https://%s.api.riotgames.com/tft/match/v1/matches/by-puuid/%s/ids?start=0&count=%d", routing, puuid, count));
                } catch (Exception e) {
                    log.warn("TFT 매치 목록 조회 실패: {}", e.getMessage());
                }
            }

            List<MatchInfo> matches = new ArrayList<>();
            if (matchIds != null) {
                for (String matchId : matchIds) {
                    try {
                        MatchInfo info = fetchTftMatchDetailWithCache(matchId, puuid, routing, req);
                        if (info != null) matches.add(info);
                    } catch (Exception e) {
                        log.warn("TFT 매치 상세 실패 - {}: {}", matchId, e.getMessage());
                    }
                }
            }

            MatchStats stats = buildStatsForSearch(matches);
            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(puuid != null ? puuid : "")
                    .gameName(req.getGameName()).tagLine(req.getTagLine())
                    .summonerLevel(String.valueOf(level))
                    .tier(tier).rank(rank).lp(lp + " LP").build();
            return PlayerSearchResponse.builder()
                    .success(true).game("tft").nickname(nickname)
                    .playerInfo(playerInfo).matches(matches).stats(stats).build();
        } catch (Exception e) {
            log.error("TFT 전적 검색 오류 - {}", req.getGameName() + "#" + req.getTagLine(), e);
            return PlayerSearchResponse.error("tft", req.getGameName() + "#" + req.getTagLine(), e.getMessage());
        }
    }

    private MatchInfo fetchTftMatchDetailWithCache(String matchId, String puuid, String routing, PlayerSearchRequest req) {
        Optional<TftMatch> row = tftMatchRepository.findByPuuidAndMatchId(puuid, matchId);
        if (row.isPresent() && !Boolean.TRUE.equals(req.getForceRefresh())
                && !MatchApiCachePolicy.isStale(row.get().getApiCachedAt())) {
            return matchInfoFromTftEntity(row.get());
        }
        Map<String, Object> match = reqMap(String.format("https://%s.api.riotgames.com/tft/match/v1/matches/%s", routing, matchId));
        MatchInfo mi = matchInfoFromTftRiotMap(match, matchId, puuid);
        if (mi != null) {
            try {
                upsertTftMatchFromRiotMap(match, puuid);
            } catch (Exception e) {
                log.warn("TFT 매치 DB 캐시 저장 실패 {}: {}", matchId, e.getMessage());
            }
        }
        return mi;
    }

    private MatchInfo matchInfoFromTftEntity(TftMatch m) {
        int placement = m.getPlacement();
        boolean win = placement <= 4;
        long ts = m.getGameCreation() != null ? m.getGameCreation() : 0L;
        String playedAt = ts > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("Asia/Seoul")).format(FORMATTER) : "";
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("placement", placement + "위");
        extras.put("level", m.getLevel());
        if (m.getTotalPlayers() != null) extras.put("totalPlayers", m.getTotalPlayers());
        return MatchInfo.builder()
                .matchId(m.getMatchId()).gameMode("TFT").win(win)
                .kills(0).deaths(0).assists(0)
                .kda((double) placement)
                .playtime(m.getGameDuration() != null ? m.getGameDuration() : 0).playedAt(playedAt).extras(extras).build();
    }

    /**
     * Records 매치 상세: TFT match v1 원본 JSON
     */
    public Map<String, Object> fetchTftMatchRawForRecords(String matchId, String regionInput) {
        if (riotApiProperties.getApiKey() == null || riotApiProperties.getApiKey().isBlank()) {
            return null;
        }
        String rawRegion = regionInput != null ? regionInput.toLowerCase() : "kr";
        String platform = PLATFORM_NORMALIZE.getOrDefault(rawRegion, rawRegion);
        String routing = REGION_ROUTING.getOrDefault(platform, "asia");
        return reqMap(String.format("https://%s.api.riotgames.com/tft/match/v1/matches/%s", routing, matchId));
    }

    public void upsertTftMatchFromRiotMapForRecords(Map<String, Object> matchRoot, String puuid) {
        upsertTftMatchFromRiotMap(matchRoot, puuid);
    }

    private void upsertTftMatchFromRiotMap(Map<String, Object> matchRoot, String puuid) {
        if (matchRoot == null) return;
        JsonNode root = objectMapper.valueToTree(matchRoot);
        JsonNode metadata = root.get("metadata");
        JsonNode info = root.get("info");
        if (metadata == null || info == null) return;
        String matchId = metadata.get("match_id").asText();
        tftMatchRepository.findByPuuidAndMatchId(puuid, matchId).ifPresent(tftMatchRepository::delete);
        JsonNode participants = info.get("participants");
        if (participants == null || !participants.isArray()) return;
        for (JsonNode p : participants) {
            if (puuid.equals(p.get("puuid").asText())) {
                TftMatch match = new TftMatch();
                match.setPuuid(puuid);
                match.setMatchId(matchId);
                match.setPlacement(p.get("placement").asInt());
                match.setLevel(p.has("level") ? p.get("level").asInt() : 0);
                match.setTotalPlayers(participants.size());
                match.setTraits(p.has("traits") ? p.get("traits").toString() : null);
                match.setUnits(p.has("units") ? p.get("units").toString() : null);
                match.setGameDuration(info.has("game_length") ? info.get("game_length").asInt() : null);
                match.setGameCreation(info.has("game_datetime") ? info.get("game_datetime").asLong() : null);
                match.setApiCachedAt(LocalDateTime.now());
                tftMatchRepository.save(match);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private MatchInfo matchInfoFromTftRiotMap(Map<String, Object> match, String matchId, String puuid) {
        if (match == null) return null;
        Map<String, Object> info = (Map<String, Object>) match.get("info");
        if (info == null) return null;
        List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");
        if (participants == null) return null;
        Map<String, Object> me = participants.stream()
                .filter(p -> puuid.equals(p.get("puuid"))).findFirst().orElse(null);
        if (me == null) return null;
        int placement = toInt(me.get("placement"));
        boolean win = placement <= 4;
        long ts = toLong(info.get("game_datetime"));
        String playedAt = ts > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("Asia/Seoul")).format(FORMATTER) : "";
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("placement", placement + "위");
        extras.put("level", me.get("level"));
        extras.put("totalDamage", me.get("total_damage_to_players"));
        extras.put("playersEliminated", me.get("players_eliminated"));
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

    private MatchStats buildStatsForSearch(List<MatchInfo> matches) {
        if (matches.isEmpty()) return MatchStats.builder().build();
        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        double avgPlacement = matches.stream().mapToDouble(m -> m.getKda() != null ? m.getKda() : 4.5).average().orElse(4.5);
        return MatchStats.builder()
                .totalGames(matches.size()).wins(wins).losses(matches.size() - wins)
                .winRate(Math.round((double) wins / matches.size() * 1000.0) / 10.0)
                .avgKda(Math.round(avgPlacement * 10.0) / 10.0).build();
    }

    private HttpEntity<Void> entityForSearch() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Riot-Token", riotApiProperties.getApiKey());
        return new HttpEntity<>(h);
    }

    private Map<String, Object> reqMap(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, entityForSearch(), new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
    }

    private List<Map<String, Object>> reqListOfMap(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, entityForSearch(), new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }

    private List<String> reqListOfString(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, entityForSearch(), new ParameterizedTypeReference<List<String>>() {}).getBody();
    }

    private int toInt(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private long toLong(Object o) { return o instanceof Number n ? n.longValue() : 0L; }
    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }
}
