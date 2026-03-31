package com.gamematcher.service.riot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.RiotApiProperties;
import com.gamematcher.dto.riot.RiotAccountResponseDto;
import com.gamematcher.dto.riot.RiotLeagueEntryResponseDto;
import com.gamematcher.dto.riot.RiotMatchDetailResponseDto;
import com.gamematcher.dto.riot.RiotStatsResponseDto;
import com.gamematcher.dto.riot.RiotSummonerResponseDto;
import com.gamematcher.entity.match.lol.LolMatchSummary;
import com.gamematcher.repository.match.MatchSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service("riotLolApiService")
@RequiredArgsConstructor
public class LolApiService {

    private static final List<String> FALLBACK_PLATFORM_BASE_URLS = List.of(
            "https://kr.api.riotgames.com",
            "https://jp1.api.riotgames.com",
            "https://na1.api.riotgames.com",
            "https://euw1.api.riotgames.com",
            "https://eun1.api.riotgames.com",
            "https://br1.api.riotgames.com",
            "https://la1.api.riotgames.com",
            "https://la2.api.riotgames.com",
            "https://oc1.api.riotgames.com",
            "https://tr1.api.riotgames.com",
            "https://ru.api.riotgames.com",
            "https://ph2.api.riotgames.com",
            "https://sg2.api.riotgames.com",
            "https://th2.api.riotgames.com",
            "https://tw2.api.riotgames.com",
            "https://vn2.api.riotgames.com"
    );

    private final RiotApiProperties riotApiProperties;
    private final MatchSummaryRepository matchSummaryRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String resolvePlatformBaseUrl(String platform) {
        if (platform == null || platform.isBlank()) {
            return riotApiProperties.getPlatformBaseUrl();
        }
        return "https://" + platform.trim().toLowerCase() + ".api.riotgames.com";
    }

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
        return getSummonerByPuuid(puuid, riotApiProperties.getPlatformBaseUrl());
    }

    public RiotSummonerResponseDto getSummonerByPuuid(String puuid, String platformBaseUrl) {
        String url = UriComponentsBuilder
                .fromHttpUrl(platformBaseUrl)
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
        return getSummonerRawByPuuid(puuid, riotApiProperties.getPlatformBaseUrl());
    }

    public String getSummonerRawByPuuid(String puuid, String platformBaseUrl) {
        String url = UriComponentsBuilder
                .fromHttpUrl(platformBaseUrl)
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

    public SummonerLookupResult findSummonerByPuuidAcrossPlatforms(String puuid) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(riotApiProperties.getPlatformBaseUrl());
        candidates.addAll(FALLBACK_PLATFORM_BASE_URLS);

        RuntimeException lastException = null;
        for (String platformBaseUrl : candidates) {
            try {
                String raw = getSummonerRawByPuuid(puuid, platformBaseUrl);
                if (raw == null || raw.isBlank()) {
                    continue;
                }

                JsonNode root = objectMapper.readTree(raw);
                JsonNode idNode = root.get("id");
                String encryptedSummonerId = idNode == null || idNode.isNull() ? null : idNode.asText();
                if (encryptedSummonerId == null || encryptedSummonerId.isBlank()) {
                    encryptedSummonerId = getSummonerIdFromLeagueEntriesByPuuid(puuid, platformBaseUrl);
                }
                if (encryptedSummonerId != null && !encryptedSummonerId.isBlank()) {
                    return new SummonerLookupResult(encryptedSummonerId, platformBaseUrl);
                }
            } catch (RuntimeException e) {
                lastException = e;
            } catch (Exception e) {
                lastException = new RuntimeException("Riot Summoner ID 조회 중 오류 발생: " + e.getMessage(), e);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        return null;
    }

    public String getEncryptedSummonerIdByPuuid(String puuid) {
        SummonerLookupResult result = findSummonerByPuuidAcrossPlatforms(puuid);
        return result == null ? null : result.encryptedSummonerId();
    }

    public String getSummonerIdFromLeagueEntriesByPuuid(String puuid, String platformBaseUrl) {
        String url = UriComponentsBuilder
                .fromHttpUrl(platformBaseUrl)
                .path("/lol/league/v4/entries/by-puuid/{puuid}")
                .buildAndExpand(puuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiProperties.getApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<RiotLeagueEntryResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<RiotLeagueEntryResponseDto>>() {}
            );
            List<RiotLeagueEntryResponseDto> entries = response.getBody();
            if (entries == null || entries.isEmpty()) {
                return null;
            }

            for (RiotLeagueEntryResponseDto entry : entries) {
                if (entry.getSummonerId() != null && !entry.getSummonerId().isBlank()) {
                    return entry.getSummonerId();
                }
            }
            return null;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw new RuntimeException("Riot League Entry API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot League Entry API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public String getSummonerIdFromRecentMatchesByPuuid(String puuid) {
        try {
            List<String> matchIds = getMatchIdsByPuuid(puuid, 0, 5);
            if (matchIds == null || matchIds.isEmpty()) {
                return null;
            }

            for (String matchId : matchIds) {
                String raw = getMatchRawByMatchId(matchId);
                if (raw == null || raw.isBlank()) {
                    continue;
                }

                JsonNode root = objectMapper.readTree(raw);
                JsonNode participants = root.path("info").path("participants");
                if (!participants.isArray()) {
                    continue;
                }

                for (JsonNode participant : participants) {
                    String participantPuuid = participant.path("puuid").asText(null);
                    if (participantPuuid != null && participantPuuid.equals(puuid)) {
                        String summonerId = participant.path("summonerId").asText(null);
                        if (summonerId != null && !summonerId.isBlank()) {
                            return summonerId;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Riot Match Detail 기반 summonerId 조회 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public String getThirdPartyCodeBySummonerId(String encryptedSummonerId) {
        return getThirdPartyCodeBySummonerId(encryptedSummonerId, riotApiProperties.getPlatformBaseUrl());
    }

    public String getThirdPartyCodeBySummonerId(String encryptedSummonerId, String platformBaseUrl) {
        String url = UriComponentsBuilder
                .fromHttpUrl(platformBaseUrl)
                .path("/lol/platform/v4/third-party-code/by-summoner/{encryptedSummonerId}")
                .buildAndExpand(encryptedSummonerId)
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
            throw new RuntimeException("Riot Third Party Code API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Riot Third Party Code API 호출 중 오류 발생: " + e.getMessage(), e);
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
            championCount.put(match.getChampionName(), championCount.getOrDefault(match.getChampionName(), 0) + 1);
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

    public record SummonerLookupResult(String encryptedSummonerId, String platformBaseUrl) {
    }
}
