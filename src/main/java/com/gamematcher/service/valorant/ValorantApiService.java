package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.ValorantApiProperties;
import com.gamematcher.dto.valorant.ValorantLifetimeApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.dto.valorant.ValorantMmrHistoryApiResponse;
import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Valorant 전적 조회 (HenrikDev API) 및 DB 저장
 * API 상세: 발로란트 api 목록 문서 기반
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValorantApiService {

    private static final Map<String, String> REGION_MAP = new HashMap<>();
    static {
        REGION_MAP.put("kr", "kr"); REGION_MAP.put("kr1", "kr");
        REGION_MAP.put("ap", "ap"); REGION_MAP.put("asia", "ap"); REGION_MAP.put("jp", "ap"); REGION_MAP.put("jp1", "ap"); REGION_MAP.put("sg", "ap");
        REGION_MAP.put("na", "na"); REGION_MAP.put("na1", "na");
        REGION_MAP.put("eu", "eu"); REGION_MAP.put("euw", "eu"); REGION_MAP.put("euw1", "eu"); REGION_MAP.put("eune1", "eu");
        REGION_MAP.put("latam", "latam"); REGION_MAP.put("la1", "latam"); REGION_MAP.put("la2", "latam");
        REGION_MAP.put("br", "br"); REGION_MAP.put("br1", "br");
    }

    private final ValorantApiProperties valorantApiProperties;
    private final ValorantMatchService valorantMatchService;
    private final ValorantMatchJsonService valorantMatchJsonService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * API URL에 api_key 쿼리 파라미터 추가 (설정된 경우)
     */
    private String appendApiKey(String url) {
        if (!valorantApiProperties.hasApiKey()) {
            return url;
        }
        return UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api_key", valorantApiProperties.getApiKey())
                .build()
                .toUriString();
    }

    /**
     * 1. 계정 정보 조회 (PUUID 확인용)
     * 형식: /valorant/v1/account/{name}/{tag}?api_key={key}
     *
     * @param name Riot Game Name
     * @param tag Riot Tag Line
     * @return 계정 정보 (puuid, region, name, tag 등)
     */
    public ValorantPuuidApiResponse getAccountByNameTag(String name, String tag) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v1/account/{name}/{tag}")
                .buildAndExpand(name, tag)
                .encode()
                .toUriString();
        url = appendApiKey(url);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return objectMapper.readValue(response.getBody(), ValorantPuuidApiResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant 계정 API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 계정 조회 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 2. 간략 전적 리스트 → 3. 매치 상세 조회 후 DB 저장
     * - v1/lifetime/matches 로 match_id 확보
     * - 각 match_id로 v2/match 상세 조회 후 저장
     *
     * @param puuid  플레이어 puuid
     * @param region 리전 (ap, kr 등)
     * @param count  저장할 매치 수 (최대 100)
     * @param mode   게임 모드 필터 (competitive, unrated 등, null이면 전체)
     */
    public void syncValorantRecentMatches(String puuid, String region, int count, String mode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v1/lifetime/matches/{region}/by-puuid/{puuid}")
                .queryParam("size", Math.min(count, 100))
                .buildAndExpand(region, puuid)
                .toUriString();
        if (mode != null && !mode.isBlank()) {
            url = UriComponentsBuilder.fromHttpUrl(url).queryParam("mode", mode).build().toUriString();
        }
        url = appendApiKey(url);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            ValorantLifetimeApiResponse lifetimeResponse = objectMapper.readValue(
                    response.getBody(),
                    ValorantLifetimeApiResponse.class
            );

            if (lifetimeResponse.getData() == null || lifetimeResponse.getData().isEmpty()) {
                return;
            }

            List<String> matchIds = lifetimeResponse.getData().stream()
                    .map(item -> item.getMeta() != null ? item.getMeta().getId() : null)
                    .filter(id -> id != null && !id.isBlank())
                    .limit(count)
                    .collect(Collectors.toList());

            for (String matchId : matchIds) {
                ValorantMatchDetailDto detail = getMatchDetail(matchId);
                if (detail != null) {
                    valorantMatchService.saveMatch(detail);
                }
            }
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant Lifetime API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 전적 동기화 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 기존 시그니처 호환용 (mode 없이 count만 전달)
     */
    public void syncValorantRecentMatches(String puuid, String region, int count) {
        syncValorantRecentMatches(puuid, region, count, null);
    }

    /**
     * 3. 매치 상세 데이터
     * 형식: /valorant/v2/match/{match_id}?api_key={key}
     *
     * @param matchId 매치 고유 ID (lifetime에서 확보)
     * @return 매치 상세 DTO, 없으면 null
     */
    public ValorantMatchDetailDto getMatchDetail(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return null;
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v2/match/{matchId}")
                .buildAndExpand(matchId)
                .toUriString();
        url = appendApiKey(url);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return valorantMatchJsonService.parseFirstMatch(response.getBody());
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw new RuntimeException("Valorant 매치 상세 API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 매치 상세 조회 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 4. 실시간 MMR 및 티어 정보
     * 형식: /valorant/v2/by-puuid/mmr/{region}/{puuid}?api_key={key}
     *
     * @param puuid  플레이어 puuid
     * @param region 리전 (ap, kr 등)
     * @return MMR/티어 정보
     */
    public ValorantMmrApiResponse getMmr(String puuid, String region) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v2/by-puuid/mmr/{region}/{puuid}")
                .buildAndExpand(region, puuid)
                .toUriString();
        url = appendApiKey(url);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return objectMapper.readValue(response.getBody(), ValorantMmrApiResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant MMR API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant MMR 조회 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 5. MMR 변동 이력 (최근 점수 그래프)
     * 형식: /valorant/v1/by-puuid/mmr-history/{region}/{puuid}?api_key={key}
     *
     * @param puuid  플레이어 puuid
     * @param region 리전 (ap, kr 등)
     * @return MMR 변동 이력
     */
    public ValorantMmrHistoryApiResponse getMmrHistory(String puuid, String region) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v1/by-puuid/mmr-history/{region}/{puuid}")
                .buildAndExpand(region, puuid)
                .toUriString();
        url = appendApiKey(url);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return objectMapper.readValue(response.getBody(), ValorantMmrHistoryApiResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant MMR History API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant MMR History 조회 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 전적 검색 (PlayerSearchResponse 반환)
     */
    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String nickname = req.getGameName() + "#" + req.getTagLine();
        String rawRegion = req.getRegion() != null ? req.getRegion().toLowerCase() : "kr";
        String region = REGION_MAP.getOrDefault(rawRegion, "ap");

        log.info("Valorant 검색 - gameName={}, tagLine={}, region(input)={}, region(mapped)={}",
                req.getGameName(), req.getTagLine(), rawRegion, region);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (valorantApiProperties.hasApiKey()) {
                headers.set("Authorization", valorantApiProperties.getApiKey().trim());
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String accountUrl = UriComponentsBuilder
                    .fromHttpUrl(valorantApiProperties.getBaseUrl())
                    .path("/valorant/v1/account/{name}/{tag}")
                    .buildAndExpand(req.getGameName(), req.getTagLine())
                    .encode()
                    .toUriString();
            if (valorantApiProperties.hasApiKey()) {
                accountUrl = appendApiKey(accountUrl);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> accountResp = restTemplate.exchange(accountUrl, HttpMethod.GET, entity, Map.class).getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> accountData = accountResp != null ? (Map<String, Object>) accountResp.get("data") : null;
            if (accountData == null) throw new RuntimeException("발로란트 계정을 찾을 수 없습니다.");

            String puuid = (String) accountData.get("puuid");
            String cardUrl = extractCardUrl(accountData);

            String tier = "UNRANKED", tierName = "";
            try {
                String mmrUrl = UriComponentsBuilder
                        .fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v2/by-puuid/mmr/{region}/{puuid}")
                        .buildAndExpand(region, puuid)
                        .toUriString();
                if (valorantApiProperties.hasApiKey()) mmrUrl = appendApiKey(mmrUrl);
                @SuppressWarnings("unchecked")
                Map<String, Object> mmrResp = restTemplate.exchange(mmrUrl, HttpMethod.GET, entity, Map.class).getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> mmrData = mmrResp != null ? (Map<String, Object>) mmrResp.get("data") : null;
                if (mmrData != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> current = (Map<String, Object>) mmrData.get("current_data");
                    if (current != null) {
                        tier = String.valueOf(current.getOrDefault("currenttier", 0));
                        tierName = (String) current.getOrDefault("currenttierpatched", "UNRANKED");
                    }
                }
            } catch (Exception e) {
                log.warn("Valorant MMR 조회 실패 (region={}): {}", region, e.getMessage());
            }

            int count = Math.min(req.getCount() != null ? req.getCount() : 5, 20);
            String matchUrl = UriComponentsBuilder
                    .fromHttpUrl(valorantApiProperties.getBaseUrl())
                    .path("/valorant/v3/by-puuid/matches/{region}/{puuid}")
                    .queryParam("size", count)
                    .buildAndExpand(region, puuid)
                    .toUriString();
            if (valorantApiProperties.hasApiKey()) matchUrl = appendApiKey(matchUrl);

            @SuppressWarnings("unchecked")
            Map<String, Object> matchResp = restTemplate.exchange(matchUrl, HttpMethod.GET, entity, Map.class).getBody();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matchData = matchResp != null ? (List<Map<String, Object>>) matchResp.get("data") : List.of();

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
                Map<String, Object> players = (Map<String, Object>) match.get("players");
                Map<String, Object> teams = (Map<String, Object>) match.get("teams");
                if (metadata == null || players == null) continue;
                List<Map<String, Object>> allPlayers = (List<Map<String, Object>>) players.get("all_players");
                if (allPlayers == null) continue;
                Map<String, Object> me = allPlayers.stream()
                        .filter(p -> puuid.equals(p.get("puuid"))).findFirst().orElse(null);
                if (me == null) continue;
                Map<String, Object> stats = (Map<String, Object>) me.get("stats");
                int kills = stats != null ? toInt(stats.get("kills")) : 0;
                int deaths = stats != null ? toInt(stats.get("deaths")) : 0;
                int assists = stats != null ? toInt(stats.get("assists")) : 0;
                double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;
                String myTeam = (String) me.get("team");
                boolean win = false;
                if (teams != null && myTeam != null) {
                    Map<String, Object> myTeamData = (Map<String, Object>) teams.get(myTeam.toLowerCase());
                    if (myTeamData != null) win = Boolean.TRUE.equals(myTeamData.get("has_won"));
                }
                Map<String, Object> extras = new LinkedHashMap<>();
                if (stats != null) {
                    extras.put("score", stats.get("score"));
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
        double avgK = matches.stream().mapToInt(m -> m.getKills() != null ? m.getKills() : 0).average().orElse(0);
        double avgD = matches.stream().mapToInt(m -> m.getDeaths() != null ? m.getDeaths() : 0).average().orElse(0);
        double avgA = matches.stream().mapToInt(m -> m.getAssists() != null ? m.getAssists() : 0).average().orElse(0);
        double avgKda = matches.stream().mapToDouble(m -> m.getKda() != null ? m.getKda() : 0).average().orElse(0);
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
        } catch (Exception e) {
            return null;
        }
    }

    private int toInt(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
