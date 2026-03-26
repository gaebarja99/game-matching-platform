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
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.match.MatchSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("riotLolApiService")
@RequiredArgsConstructor
public class LolApiService {

    private final RiotApiProperties riotApiProperties;
    private final MatchSummaryRepository matchSummaryRepository;
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
            int code = e.getStatusCode().value();
            if (code == 401 || code == 403) {
                throw new GameApiException(HttpStatus.BAD_GATEWAY,
                        "Riot API 키가 거부되었습니다. 환경 변수 RIOT_API_KEY 또는 riot.api.key에 개발자 포털에서 발급·갱신한 키를 넣어 주세요.");
            }
            if (code == 404) {
                throw new GameApiException(HttpStatus.NOT_FOUND, "해당 Riot ID(게임명#태그)를 찾을 수 없습니다.");
            }
            throw new GameApiException(HttpStatus.BAD_GATEWAY,
                    "Riot Account API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new GameApiException(HttpStatus.BAD_GATEWAY, "Riot Account API 호출 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 랭크 큐별 티어(솔로/자유 등). 비배치·API 오류 시 빈 리스트 (연동 프로필 표시용).
     */
    public List<RiotLeagueEntryResponseDto> findLeagueEntriesByPuuidOrEmpty(String puuid) {
        if (puuid == null || puuid.isBlank()) {
            return List.of();
        }
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiProperties.getPlatformBaseUrl())
                .path("/lol/league/v4/entries/by-puuid/{encryptedPUUID}")
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
            List<RiotLeagueEntryResponseDto> body = response.getBody();
            return body != null ? body : List.of();
        } catch (HttpStatusCodeException e) {
            log.debug("LoL league-v4 조회 생략: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.debug("LoL league-v4 조회 오류(무시): {}", e.getMessage());
            return List.of();
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
}
