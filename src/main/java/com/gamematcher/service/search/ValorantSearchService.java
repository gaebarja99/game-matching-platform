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
 * Valorant 전적 검색 서비스
 * Henrik Dev API v3 사용 (https://docs.henrikdev.xyz/)
 *
 * 지원 리전: ap(아시아태평양), kr(한국), na(북미), eu(유럽), latam(남미), br(브라질)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValorantSearchService {

    private final RestTemplate restTemplate;

    @Value("${valorant.api.key:}")
    private String henrikApiKey;

    // 입력값 → Henrik API 리전 코드 정규화
    private static final Map<String, String> REGION_MAP = new HashMap<>();
    static {
        // 한국/아시아
        REGION_MAP.put("kr",    "kr");
        REGION_MAP.put("kr1",   "kr");
        REGION_MAP.put("ap",    "ap");
        REGION_MAP.put("asia",  "ap");
        REGION_MAP.put("jp",    "ap");
        REGION_MAP.put("jp1",   "ap");
        REGION_MAP.put("sg",    "ap");
        // 북미
        REGION_MAP.put("na",    "na");
        REGION_MAP.put("na1",   "na");
        // 유럽
        REGION_MAP.put("eu",    "eu");
        REGION_MAP.put("euw",   "eu");
        REGION_MAP.put("euw1",  "eu");
        REGION_MAP.put("eune1", "eu");
        // 남미
        REGION_MAP.put("latam", "latam");
        REGION_MAP.put("la1",   "latam");
        REGION_MAP.put("la2",   "latam");
        // 브라질
        REGION_MAP.put("br",    "br");
        REGION_MAP.put("br1",   "br");
    }

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String nickname = req.getGameName() + "#" + req.getTagLine();
        String rawRegion = req.getRegion() != null ? req.getRegion().toLowerCase() : "kr";
        String region    = REGION_MAP.getOrDefault(rawRegion, "ap");

        log.info("Valorant 검색 - gameName={}, tagLine={}, region(input)={}, region(mapped)={}",
                req.getGameName(), req.getTagLine(), rawRegion, region);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (henrikApiKey != null && !henrikApiKey.isBlank()) {
                headers.set("Authorization", henrikApiKey.trim());
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 1) 계정 정보 (region 무관)
            String accountUrl = String.format(
                    "https://api.henrikdev.xyz/valorant/v1/account/%s/%s",
                    req.getGameName(), req.getTagLine());
            log.info("accountUrl={}", accountUrl);

            @SuppressWarnings("unchecked")
            Map<String, Object> accountResp = restTemplate.exchange(
                    accountUrl, HttpMethod.GET, entity, Map.class).getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> accountData = accountResp != null
                    ? (Map<String, Object>) accountResp.get("data") : null;
            if (accountData == null) throw new RuntimeException("발로란트 계정을 찾을 수 없습니다.");

            String puuid   = (String) accountData.get("puuid");
            String cardUrl = extractCardUrl(accountData);

            // 2) MMR (랭크)
            String tier = "UNRANKED", tierName = "";
            try {
                String mmrUrl = String.format(
                        "https://api.henrikdev.xyz/valorant/v2/by-puuid/mmr/%s/%s",
                        region, puuid);
                @SuppressWarnings("unchecked")
                Map<String, Object> mmrResp = restTemplate.exchange(
                        mmrUrl, HttpMethod.GET, entity, Map.class).getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> mmrData = mmrResp != null
                        ? (Map<String, Object>) mmrResp.get("data") : null;
                if (mmrData != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> current = (Map<String, Object>) mmrData.get("current_data");
                    if (current != null) {
                        tier     = String.valueOf(current.getOrDefault("currenttier", 0));
                        tierName = (String) current.getOrDefault("currenttierpatched", "UNRANKED");
                    }
                }
            } catch (Exception e) {
                log.warn("Valorant MMR 조회 실패 (region={}): {}", region, e.getMessage());
            }

            // 3) 최근 매치 (by-puuid → region 필요)
            int count = Math.min(req.getCount() != null ? req.getCount() : 5, 20);
            String matchUrl = String.format(
                    "https://api.henrikdev.xyz/valorant/v3/by-puuid/matches/%s/%s?size=%d",
                    region, puuid, count);
            log.info("matchUrl={}", matchUrl);

            @SuppressWarnings("unchecked")
            Map<String, Object> matchResp = restTemplate.exchange(
                    matchUrl, HttpMethod.GET, entity, Map.class).getBody();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matchData = matchResp != null
                    ? (List<Map<String, Object>>) matchResp.get("data") : List.of();

            List<MatchInfo> matches = parseMatches(matchData, puuid);
            MatchStats stats = buildStats(matches);

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(puuid).gameName(req.getGameName()).tagLine(req.getTagLine())
                    .tier(tierName.isEmpty() ? tier : tierName)
                    .avatarUrl(cardUrl).build();

            return PlayerSearchResponse.builder()
                    .success(true).game("valorant").nickname(nickname)
                    .playerInfo(playerInfo).matches(matches).stats(stats).build();

        } catch (Exception e) {
            log.error("Valorant 전적 검색 오류 - {}", nickname, e);
            return PlayerSearchResponse.error("valorant", nickname, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<MatchInfo> parseMatches(List<Map<String, Object>> matchData, String puuid) {
        if (matchData == null) return List.of();
        List<MatchInfo> result = new ArrayList<>();
        for (Map<String, Object> match : matchData) {
            try {
                Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");
                Map<String, Object> players  = (Map<String, Object>) match.get("players");
                Map<String, Object> teams    = (Map<String, Object>) match.get("teams");
                if (metadata == null || players == null) continue;

                List<Map<String, Object>> allPlayers = (List<Map<String, Object>>) players.get("all_players");
                if (allPlayers == null) continue;

                Map<String, Object> me = allPlayers.stream()
                        .filter(p -> puuid.equals(p.get("puuid"))).findFirst().orElse(null);
                if (me == null) continue;

                Map<String, Object> stats = (Map<String, Object>) me.get("stats");
                int kills   = stats != null ? toInt(stats.get("kills"))   : 0;
                int deaths  = stats != null ? toInt(stats.get("deaths"))  : 0;
                int assists = stats != null ? toInt(stats.get("assists")) : 0;
                double kda  = deaths == 0 ? (kills + assists) : (double)(kills + assists) / deaths;

                String myTeam = (String) me.get("team");
                boolean win = false;
                if (teams != null && myTeam != null) {
                    Map<String, Object> myTeamData = (Map<String, Object>) teams.get(myTeam.toLowerCase());
                    if (myTeamData != null) win = Boolean.TRUE.equals(myTeamData.get("has_won"));
                }

                Map<String, Object> extras = new LinkedHashMap<>();
                if (stats != null) {
                    extras.put("score",     stats.get("score"));
                    extras.put("headshots", stats.get("headshots"));
                    extras.put("bodyshots", stats.get("bodyshots"));
                }

                result.add(MatchInfo.builder()
                        .matchId((String) metadata.get("matchid"))
                        .gameMode((String) metadata.getOrDefault("mode", ""))
                        .agent((String) me.getOrDefault("character", ""))
                        .win(win).kills(kills).deaths(deaths).assists(assists)
                        .kda(Math.round(kda * 100.0) / 100.0)
                        .playedAt((String) metadata.getOrDefault("game_start_patched", ""))
                        .extras(extras).build());
            } catch (Exception e) {
                log.warn("Valorant 매치 파싱 실패", e);
            }
        }
        return result;
    }

    private MatchStats buildStats(List<MatchInfo> matches) {
        if (matches.isEmpty()) return MatchStats.builder().build();
        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        double avgK   = matches.stream().mapToInt(m -> m.getKills()   != null ? m.getKills()   : 0).average().orElse(0);
        double avgD   = matches.stream().mapToInt(m -> m.getDeaths()  != null ? m.getDeaths()  : 0).average().orElse(0);
        double avgA   = matches.stream().mapToInt(m -> m.getAssists() != null ? m.getAssists() : 0).average().orElse(0);
        double avgKda = matches.stream().mapToDouble(m -> m.getKda()  != null ? m.getKda()     : 0).average().orElse(0);
        String mostAgent = matches.stream().filter(m -> m.getAgent() != null)
                .collect(Collectors.groupingBy(MatchInfo::getAgent, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("-");
        return MatchStats.builder()
                .totalGames(matches.size()).wins(wins).losses(matches.size() - wins)
                .winRate(Math.round((double) wins / matches.size() * 1000.0) / 10.0)
                .avgKills(Math.round(avgK * 10.0) / 10.0).avgDeaths(Math.round(avgD * 10.0) / 10.0)
                .avgAssists(Math.round(avgA * 10.0) / 10.0).avgKda(Math.round(avgKda * 100.0) / 100.0)
                .mostUsedChampionOrAgent(mostAgent).build();
    }

    @SuppressWarnings("unchecked")
    private String extractCardUrl(Map<String, Object> data) {
        try {
            Map<String, Object> card = (Map<String, Object>) data.get("card");
            return card != null ? (String) card.get("small") : null;
        } catch (Exception e) { return null; }
    }

    private int toInt(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }
}
