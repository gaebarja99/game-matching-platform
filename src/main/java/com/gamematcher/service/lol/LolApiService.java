package com.gamematcher.service.lol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.RiotApiProperties;
import com.gamematcher.service.MatchApiCachePolicy;
import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.riot.RiotAccountResponseDto;
import com.gamematcher.dto.riot.RiotMatchDetailResponseDto;
import com.gamematcher.dto.riot.RiotStatsResponseDto;
import com.gamematcher.dto.riot.RiotSummonerResponseDto;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import com.gamematcher.entity.match.lol.LolMatch;
import com.gamematcher.entity.match.lol.LolMatchParticipant;
import com.gamematcher.entity.match.lol.LolMatchSummary;
import com.gamematcher.repository.match.LolMatchRepository;
import com.gamematcher.repository.match.MatchSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * LoL 전적 조회 및 DB 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LolApiService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<String, String> PLATFORM = new HashMap<>();
    private static final Map<String, String> ROUTING = new HashMap<>();
    private static final Map<Integer, String> QUEUE_LABEL = new HashMap<>();
    static {
        PLATFORM.put("kr", "kr"); PLATFORM.put("kr1", "kr");
        PLATFORM.put("jp", "jp1"); PLATFORM.put("jp1", "jp1");
        PLATFORM.put("na", "na1"); PLATFORM.put("na1", "na1");
        PLATFORM.put("euw", "euw1"); PLATFORM.put("euw1", "euw1");
        PLATFORM.put("eune", "eune1"); PLATFORM.put("eune1", "eune1");
        PLATFORM.put("br", "br1"); PLATFORM.put("br1", "br1");
        PLATFORM.put("la1", "la1"); PLATFORM.put("la2", "la2");
        PLATFORM.put("tr", "tr1"); PLATFORM.put("tr1", "tr1");
        PLATFORM.put("ru", "ru"); PLATFORM.put("oc1", "oc1");
        ROUTING.put("kr", "asia"); ROUTING.put("jp1", "asia");
        ROUTING.put("na1", "americas"); ROUTING.put("la1", "americas");
        ROUTING.put("la2", "americas"); ROUTING.put("br1", "americas");
        ROUTING.put("euw1", "europe"); ROUTING.put("eune1", "europe");
        ROUTING.put("tr1", "europe"); ROUTING.put("ru", "europe");
        ROUTING.put("oc1", "sea");
        QUEUE_LABEL.put(420, "RANKED_SOLO_5x5"); QUEUE_LABEL.put(440, "RANKED_FLEX_SR");
        QUEUE_LABEL.put(450, "ARAM"); QUEUE_LABEL.put(400, "NORMAL_DRAFT");
        QUEUE_LABEL.put(430, "NORMAL_BLIND"); QUEUE_LABEL.put(490, "QUICKPLAY");
        QUEUE_LABEL.put(700, "CLASH");
    }

    private final RiotApiProperties riotApiProperties;
    private final MatchSummaryRepository matchSummaryRepository;
    private final LolMatchRepository lolMatchRepository;
    private final LolMatchService lolMatchService;
    private final LolMatchJsonService lolMatchJsonService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public RiotAccountResponseDto getAccountByRiotId(String gameName, String tagLine) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getRegionalBaseUrl())
                .path("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
                .buildAndExpand(gameName, tagLine)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiProperties.getApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<RiotAccountResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RiotAccountResponseDto.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Riot Account API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot Account API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public RiotSummonerResponseDto getSummonerByPuuid(String puuid) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getPlatformBaseUrl())
                .path("/lol/summoner/v4/summoners/by-puuid/{puuid}")
                .buildAndExpand(puuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiProperties.getApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<RiotSummonerResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RiotSummonerResponseDto.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Riot Summoner API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot Summoner API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public String getSummonerRawByPuuid(String puuid) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getPlatformBaseUrl())
                .path("/lol/summoner/v4/summoners/by-puuid/{puuid}")
                .buildAndExpand(puuid)
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
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Riot Summoner Raw API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot Summoner Raw API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public List<String> getMatchIdsByPuuid(String puuid, int start, int count) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getRegionalBaseUrl())
                .path("/lol/match/v5/matches/by-puuid/{puuid}/ids")
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
                    new ParameterizedTypeReference<List<String>>() {}
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Riot MatchIds API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot MatchIds API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public String getMatchRawByMatchId(String matchId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getRegionalBaseUrl())
                .path("/lol/match/v5/matches/{matchId}")
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
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Riot Match Detail API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot Match Detail API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 매치 타임라인 조회 (분 단위 프레임, 골드·CS·레벨 변화 등).
     * 형식: {regional_base_url}/lol/match/v5/matches/{matchId}/timeline
     */
    public String getMatchTimelineRawByMatchId(String matchId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getRegionalBaseUrl())
                .path("/lol/match/v5/matches/{matchId}/timeline")
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
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Riot Match Timeline API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot Match Timeline API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public RiotMatchDetailResponseDto getMatchDetailByMatchId(String matchId, String puuid) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getRegionalBaseUrl())
                .path("/lol/match/v5/matches/{matchId}")
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

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode info = root.get("info");
            JsonNode participants = info.get("participants");

            RiotMatchDetailResponseDto result = new RiotMatchDetailResponseDto();
            result.setMatchId(root.get("metadata").get("matchId").asText());
            result.setGameMode(info.get("gameMode").asText());
            result.setGameDuration(info.get("gameDuration").asLong());

            for (JsonNode participant : participants) {
                if (puuid.equals(participant.get("puuid").asText())) {
                    result.setChampionName(participant.get("championName").asText());
                    result.setWin(participant.get("win").asBoolean());
                    result.setKills(participant.get("kills").asInt());
                    result.setDeaths(participant.get("deaths").asInt());
                    result.setAssists(participant.get("assists").asInt());
                    return result;
                }
            }

            throw new RuntimeException("해당 puuid를 match participants에서 찾을 수 없습니다.");
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Riot Match Detail API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot Match Detail API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public List<RiotMatchDetailResponseDto> getRecentMatchDetailsByPuuid(String puuid, int count) {
        List<String> matchIds = getMatchIdsByPuuid(puuid, 0, count);

        if (matchIds == null || matchIds.isEmpty()) {
            return List.of();
        }

        List<RiotMatchDetailResponseDto> results = new ArrayList<>();

        for (String matchId : matchIds) {
            RiotMatchDetailResponseDto detail = getMatchDetailByMatchId(matchId, puuid);
            results.add(detail);
        }

        return results;
    }

    public RiotStatsResponseDto getRecentStats(String puuid, String gameName, String tagLine) {
        List<RiotMatchDetailResponseDto> matches = getRecentMatchDetailsByPuuid(puuid, 5);

        int games = matches.size();

        if (games == 0) {
            RiotStatsResponseDto emptyStats = new RiotStatsResponseDto();
            emptyStats.setPuuid(puuid);
            emptyStats.setGameName(gameName);
            emptyStats.setTagLine(tagLine);
            emptyStats.setGames(0);
            emptyStats.setWins(0);
            emptyStats.setLosses(0);
            emptyStats.setWinRate(0.0);
            emptyStats.setAvgKills(0.0);
            emptyStats.setAvgDeaths(0.0);
            emptyStats.setAvgAssists(0.0);
            emptyStats.setMostPlayedChampion("None");
            return emptyStats;
        }

        int wins = 0;
        int kills = 0;
        int deaths = 0;
        int assists = 0;

        Map<String, Integer> championCount = new HashMap<>();

        for (RiotMatchDetailResponseDto match : matches) {
            if (match.isWin()) {
                wins++;
            }

            kills += match.getKills();
            deaths += match.getDeaths();
            assists += match.getAssists();

            championCount.put(
                    match.getChampionName(),
                    championCount.getOrDefault(match.getChampionName(), 0) + 1
            );
        }

        String mostPlayed = championCount.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        RiotStatsResponseDto stats = new RiotStatsResponseDto();
        stats.setPuuid(puuid);
        stats.setGameName(gameName);
        stats.setTagLine(tagLine);
        stats.setGames(games);
        stats.setWins(wins);
        stats.setLosses(games - wins);
        stats.setWinRate((wins * 100.0) / games);
        stats.setAvgKills(kills / (double) games);
        stats.setAvgDeaths(deaths / (double) games);
        stats.setAvgAssists(assists / (double) games);
        stats.setMostPlayedChampion(mostPlayed);

        return stats;
    }

    public void saveMatchSummary(
            String puuid,
            String matchId,
            String championName,
            int kills,
            int deaths,
            int assists,
            boolean win,
            String teamPosition,
            int totalDamage,
            int visionScore,
            int cs,
            int gameDuration,
            long gameCreation
    ) {
        if (matchSummaryRepository.existsByPuuidAndMatchId(puuid, matchId)) {
            return;
        }

        LolMatchSummary match = new LolMatchSummary();
        match.setPuuid(puuid);
        match.setMatchId(matchId);
        match.setChampionName(championName);
        match.setKills(kills);
        match.setDeaths(deaths);
        match.setAssists(assists);
        match.setWin(win);
        match.setTeamPosition(teamPosition);
        match.setTotalDamage(totalDamage);
        match.setVisionScore(visionScore);
        match.setCs(cs);
        match.setGameDuration(gameDuration);
        match.setGameCreation(gameCreation);

        matchSummaryRepository.save(match);
    }

    public void syncRecentMatches(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid, 0, 5);

        if (matchIds == null || matchIds.isEmpty()) {
            return;
        }

        for (String matchId : matchIds) {
            RiotMatchDetailResponseDto detail = getMatchDetailByMatchId(matchId, puuid);

            saveMatchSummary(
                    puuid,
                    detail.getMatchId(),
                    detail.getChampionName(),
                    detail.getKills(),
                    detail.getDeaths(),
                    detail.getAssists(),
                    detail.isWin(),
                    "UNKNOWN",
                    0,
                    0,
                    0,
                    (int) detail.getGameDuration(),
                    System.currentTimeMillis()
            );
        }
    }

    /**
     * 전적 검색 (PlayerSearchResponse 반환)
     * LolSearchService 로직 통합 - 리전별 동적 URL 사용
     */
    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String tagLine = req.getTagLine() != null ? req.getTagLine().trim() : "";
        String nickname = req.getGameName() + (tagLine.isBlank() ? "" : "#" + tagLine);

        if (riotApiProperties.getApiKey() == null || riotApiProperties.getApiKey().isBlank()) {
            return PlayerSearchResponse.error("lol", nickname, "Riot API 키가 설정되지 않았습니다.");
        }

        try {
            String plat = PLATFORM.getOrDefault(req.getRegion() != null ? req.getRegion().toLowerCase() : "kr", "kr");
            String rout = ROUTING.getOrDefault(plat, "asia");

            log.info("▶ LoL 검색 - gameName={} tagLine={} platform={} routing={}", req.getGameName(), tagLine, plat, rout);

            String puuid = fetchPuuidForSearch(rout, req.getGameName(), tagLine);
            if (puuid == null) {
                return PlayerSearchResponse.error("lol", nickname, "계정을 찾을 수 없습니다. 닉네임#태그 형식을 확인하세요.");
            }

            int level = 0;
            Object iconId = 0;
            String sumId = null;
            try {
                Map<String, Object> sd = getMapForSearch("https://" + plat + ".api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + puuid);
                if (sd != null) {
                    level = toIntForSearch(sd.get("summonerLevel"));
                    iconId = sd.getOrDefault("profileIconId", 0);
                    sumId = (String) sd.get("id");
                }
            } catch (Exception e) {
                log.warn("[STEP2] 소환사 정보 실패 (무시하고 계속): {}", e.getMessage());
            }

            String tier = "UNRANKED", rank = "", lp = "0";
            int rWins = 0, rLoss = 0;
            try {
                String rankUrl = "https://" + plat + ".api.riotgames.com/lol/league/v4/entries/by-puuid/" + puuid;
                List<Map<String, Object>> rankList = getListOfMapForSearch(rankUrl);
                if (rankList != null && !rankList.isEmpty()) {
                    for (Map<String, Object> e : rankList) {
                        if ("RANKED_SOLO_5x5".equals(e.get("queueType"))) {
                            tier = String.valueOf(e.getOrDefault("tier", "UNRANKED"));
                            rank = String.valueOf(e.getOrDefault("rank", ""));
                            lp = String.valueOf(e.getOrDefault("leaguePoints", 0));
                            rWins = toIntForSearch(e.get("wins"));
                            rLoss = toIntForSearch(e.get("losses"));
                            if (sumId == null) sumId = (String) e.get("summonerId");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[STEP3] 랭크 조회 실패: {}", e.getMessage());
            }

            int count = Math.min(req.getCount() != null ? req.getCount() : 5, 20);
            Integer qId = req.getQueueType() != null ? req.getQueueType() : 420;
            List<String> matchIds = fetchMatchIdsForSearch(rout, puuid, qId, count);
            if (matchIds.isEmpty() && qId == 420) {
                matchIds = getListOfStringForSearch(String.format("https://%s.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=%d", rout, puuid, count));
                if (matchIds == null) matchIds = new ArrayList<>();
            }

            List<MatchInfo> matches = new ArrayList<>();
            for (String mId : matchIds) {
                try {
                    MatchInfo mi = null;
                    Optional<LolMatch> cached = lolMatchRepository.findByMatchIdWithParticipants(mId);
                    if (cached.isPresent() && !Boolean.TRUE.equals(req.getForceRefresh())
                            && !MatchApiCachePolicy.isStale(cached.get().getApiCachedAt())) {
                        mi = matchInfoFromLolEntity(cached.get(), puuid);
                    }
                    if (mi == null) {
                        Map<String, Object> raw = getMapForSearch("https://" + rout + ".api.riotgames.com/lol/match/v5/matches/" + mId);
                        if (raw != null) {
                            mi = matchInfoFromLolRiotMap(raw, mId, puuid);
                            if (mi != null) {
                                persistLolMatchFromRiotMap(raw);
                            }
                        }
                    }
                    if (mi != null) matches.add(mi);
                } catch (Exception e) {
                    log.warn("매치 상세 실패 - {}: {}", mId, e.getMessage());
                }
            }

            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("summonerId", sumId);
            rawData.put("rankedWins", rWins);
            rawData.put("rankedLosses", rLoss);
            if (rWins + rLoss > 0) {
                rawData.put("rankedWinRate", Math.round((double) rWins / (rWins + rLoss) * 1000.0) / 10.0 + "%");
            }

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(puuid).gameName(req.getGameName()).tagLine(tagLine)
                    .summonerLevel(String.valueOf(level)).profileIconId(String.valueOf(iconId))
                    .tier(tier).rank(rank).lp(lp + " LP")
                    .rawData(rawData).build();

            return PlayerSearchResponse.builder()
                    .success(true).game("lol").nickname(nickname)
                    .playerInfo(playerInfo).matches(matches).stats(buildStatsForSearch(matches))
                    .build();
        } catch (Exception e) {
            log.error("LoL 검색 오류 - {}: {}", nickname, e.getMessage(), e);
            return PlayerSearchResponse.error("lol", nickname, e.getMessage());
        }
    }

    private String fetchPuuidForSearch(String routing, String gameName, String tagLine) {
        try {
            String url = String.format("https://%s.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s", routing, gameName, tagLine);
            Map<String, Object> data = getMapForSearch(url);
            return data != null ? (String) data.get("puuid") : null;
        } catch (HttpClientErrorException e) {
            log.error("[STEP1] HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("[STEP1] 예외: {}", e.getMessage());
            return null;
        }
    }

    private List<String> fetchMatchIdsForSearch(String routing, String puuid, int queueId, int count) {
        List<String> result = new ArrayList<>();
        try {
            if (queueId == -1) {
                int perQueue = Math.max(count / 2, 3);
                for (int q : new int[]{400, 430, 490}) {
                    String url = String.format("https://%s.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?queue=%d&start=0&count=%d", routing, puuid, q, perQueue);
                    List<String> ids = getListOfStringForSearch(url);
                    if (ids != null) result.addAll(ids);
                }
                result = result.stream().distinct().limit(count).collect(Collectors.toList());
            } else {
                String url = String.format("https://%s.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?queue=%d&start=0&count=%d", routing, puuid, queueId, count);
                List<String> ids = getListOfStringForSearch(url);
                if (ids != null) result.addAll(ids);
            }
        } catch (Exception e) {
            log.error("[STEP4] 매치 목록 오류: {}", e.getMessage());
        }
        return result;
    }

    private void persistLolMatchFromRiotMap(Map<String, Object> match) {
        try {
            String json = objectMapper.writeValueAsString(match);
            LolMatchDetailDto dto = lolMatchJsonService.parseMatchDetail(json);
            lolMatchService.replaceMatchFromApi(dto);
        } catch (Exception e) {
            log.warn("LoL 매치 DB 캐시 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * Records 매치 상세 패널: Riot match-v5 원본 JSON 조회
     */
    public Map<String, Object> fetchMatchV5RawForRecords(String matchId, String regionInput) {
        if (riotApiProperties.getApiKey() == null || riotApiProperties.getApiKey().isBlank()) {
            return null;
        }
        String plat = PLATFORM.getOrDefault(regionInput != null ? regionInput.toLowerCase(java.util.Locale.ROOT) : "kr", "kr");
        String rout = ROUTING.getOrDefault(plat, "asia");
        return getMapForSearch("https://" + rout + ".api.riotgames.com/lol/match/v5/matches/" + matchId);
    }

    /** Records 매치 상세 조회 후 DB 캐시 저장 */
    public void persistMatchV5FromSearchMap(Map<String, Object> match) {
        persistLolMatchFromRiotMap(match);
    }

    private MatchInfo matchInfoFromLolEntity(LolMatch m, String puuid) {
        if (m.getParticipants() == null) return null;
        LolMatchParticipant me = m.getParticipants().stream()
                .filter(p -> puuid.equals(p.getPuuid())).findFirst().orElse(null);
        if (me == null) return null;
        int kills = me.getKills() != null ? me.getKills() : 0;
        int deaths = me.getDeaths() != null ? me.getDeaths() : 0;
        int assists = me.getAssists() != null ? me.getAssists() : 0;
        double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;
        long gameEnd = m.getGameEndTimestamp() != null ? m.getGameEndTimestamp() : 0L;
        String playedAt = gameEnd > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(gameEnd), ZoneId.of("Asia/Seoul")).format(FORMATTER) : "";
        int queueIdVal = m.getQueueId() != null ? m.getQueueId() : 0;
        String gameMode = QUEUE_LABEL.getOrDefault(queueIdVal, "CLASSIC");
        int cs = (me.getTotalMinionsKilled() != null ? me.getTotalMinionsKilled() : 0)
                + (me.getNeutralMinionsKilled() != null ? me.getNeutralMinionsKilled() : 0);
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("queueId", queueIdVal);
        extras.put("goldEarned", me.getGoldEarned());
        extras.put("totalDamage", me.getTotalDamageDealtToChampions());
        extras.put("visionScore", me.getVisionScore());
        return MatchInfo.builder()
                .matchId(m.getMatchId()).gameMode(gameMode).champion(me.getChampionName() != null ? me.getChampionName() : "")
                .win(me.isWin())
                .kills(kills).deaths(deaths).assists(assists).kda(Math.round(kda * 100.0) / 100.0)
                .cs(cs)
                .playtime(m.getGameDuration() != null ? m.getGameDuration().intValue() : 0).playedAt(playedAt).extras(extras).build();
    }

    @SuppressWarnings("unchecked")
    private MatchInfo matchInfoFromLolRiotMap(Map<String, Object> match, String matchId, String puuid) {
        if (match == null) return null;
        Map<String, Object> info = (Map<String, Object>) match.get("info");
        if (info == null) return null;
        List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");
        if (participants == null) return null;
        Map<String, Object> me = participants.stream()
                .filter(p -> puuid.equals(p.get("puuid"))).findFirst().orElse(null);
        if (me == null) return null;
        int kills = toIntForSearch(me.get("kills"));
        int deaths = toIntForSearch(me.get("deaths"));
        int assists = toIntForSearch(me.get("assists"));
        double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;
        long gameEnd = toLongForSearch(info.get("gameEndTimestamp"));
        String playedAt = gameEnd > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(gameEnd), ZoneId.of("Asia/Seoul")).format(FORMATTER) : "";
        int queueIdVal = toIntForSearch(info.get("queueId"));
        String gameMode = QUEUE_LABEL.getOrDefault(queueIdVal, "CLASSIC");
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("queueId", queueIdVal);
        extras.put("goldEarned", me.get("goldEarned"));
        extras.put("totalDamage", me.get("totalDamageDealtToChampions"));
        extras.put("wardPlaced", me.get("wardsPlaced"));
        extras.put("visionScore", me.get("visionScore"));
        return MatchInfo.builder()
                .matchId(matchId).gameMode(gameMode).champion((String) me.getOrDefault("championName", ""))
                .win((Boolean) me.getOrDefault("win", false))
                .kills(kills).deaths(deaths).assists(assists).kda(Math.round(kda * 100.0) / 100.0)
                .cs(toIntForSearch(me.get("totalMinionsKilled")) + toIntForSearch(me.get("neutralMinionsKilled")))
                .playtime(toIntForSearch(info.get("gameDuration"))).playedAt(playedAt).extras(extras).build();
    }

    private MatchStats buildStatsForSearch(List<MatchInfo> matches) {
        if (matches.isEmpty()) return MatchStats.builder().build();
        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        double avgK = matches.stream().mapToInt(m -> m.getKills() != null ? m.getKills() : 0).average().orElse(0);
        double avgD = matches.stream().mapToInt(m -> m.getDeaths() != null ? m.getDeaths() : 0).average().orElse(0);
        double avgA = matches.stream().mapToInt(m -> m.getAssists() != null ? m.getAssists() : 0).average().orElse(0);
        double avgKda = matches.stream().mapToDouble(m -> m.getKda() != null ? m.getKda() : 0).average().orElse(0);
        String most = matches.stream().filter(m -> m.getChampion() != null && !m.getChampion().isBlank())
                .collect(Collectors.groupingBy(MatchInfo::getChampion, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("-");
        return MatchStats.builder()
                .totalGames(matches.size()).wins(wins).losses(matches.size() - wins)
                .winRate(Math.round((double) wins / matches.size() * 1000.0) / 10.0)
                .avgKills(Math.round(avgK * 10.0) / 10.0).avgDeaths(Math.round(avgD * 10.0) / 10.0)
                .avgAssists(Math.round(avgA * 10.0) / 10.0).avgKda(Math.round(avgKda * 100.0) / 100.0)
                .mostUsedChampionOrAgent(most).build();
    }

    private HttpEntity<Void> entityForSearch() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Riot-Token", riotApiProperties.getApiKey());
        return new HttpEntity<>(h);
    }

    private Map<String, Object> getMapForSearch(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, entityForSearch(), new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
    }

    private List<Map<String, Object>> getListOfMapForSearch(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, entityForSearch(), new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }

    private List<String> getListOfStringForSearch(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, entityForSearch(), new ParameterizedTypeReference<List<String>>() {}).getBody();
    }

    private int toIntForSearch(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private long toLongForSearch(Object o) { return o instanceof Number n ? n.longValue() : 0L; }
}
