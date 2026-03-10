package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.ValorantApiProperties;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.repository.match.ValorantMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Valorant 전적 조회 (HenrikDev API) 및 DB 저장
 */
@Service
@RequiredArgsConstructor
public class ValorantApiService {

    private final ValorantApiProperties valorantApiProperties;
    private final ValorantMatchRepository valorantMatchRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void syncValorantRecentMatches(String puuid, String region, int count) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v3/by-puuid/matches/{region}/{puuid}")
                .buildAndExpand(region, puuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        if (valorantApiProperties.hasApiKey()) {
            headers.set("Authorization", valorantApiProperties.getApiKey());
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return;

            int saved = 0;
            for (JsonNode matchNode : data) {
                if (saved >= count) break;

                JsonNode metadata = matchNode.get("metadata");
                if (metadata == null || !metadata.has("matchid")) continue;
                String matchId = metadata.get("matchid").asText();
                if (valorantMatchRepository.existsByPuuidAndMatchId(puuid, matchId)) continue;

                JsonNode players = matchNode.get("players");
                if (players == null || !players.isObject()) continue;
                JsonNode allPlayers = players.get("all_players");
                if (allPlayers == null || !allPlayers.isArray()) continue;

                for (JsonNode p : allPlayers) {
                    if (!puuid.equals(p.path("puuid").asText(""))) continue;

                    boolean win = false;
                    String team = p.path("team").asText("");
                    JsonNode red = players.path("red");
                    JsonNode blue = players.path("blue");
                    if ("Red".equalsIgnoreCase(team) && red.path("has_won").asBoolean(false)) win = true;
                    else if ("Blue".equalsIgnoreCase(team) && blue.path("has_won").asBoolean(false)) win = true;

                    ValorantMatch match = new ValorantMatch();
                    match.setPuuid(puuid);
                    match.setMatchId(matchId);
                    match.setAgent(p.has("character") ? p.get("character").asText() : null);
                    JsonNode stats = p.path("stats");
                    match.setKills(stats.path("kills").asInt(0));
                    match.setDeaths(stats.path("deaths").asInt(0));
                    match.setAssists(stats.path("assists").asInt(0));
                    match.setWin(win);
                    match.setMap(metadata.has("map") ? metadata.get("map").asText() : null);
                    if (metadata.has("rounds_played")) {
                        int rounds = metadata.get("rounds_played").asInt(0);
                        int roundsWon = stats.path("rounds_won").asInt(0);
                        match.setRoundsWon(roundsWon);
                        match.setRoundsLost(rounds - roundsWon);
                    }
                    valorantMatchRepository.save(match);
                    saved++;
                    break;
                }
            }
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 전적 동기화 오류: " + e.getMessage(), e);
        }
    }
}
