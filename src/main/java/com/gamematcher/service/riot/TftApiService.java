package com.gamematcher.service.riot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.RiotApiProperties;
import com.gamematcher.entity.match.TftMatch;
import com.gamematcher.repository.match.TftMatchRepository;
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

import java.util.List;

/**
 * TFT 전적 조회 및 DB 저장
 */
@Service("riotTftApiService")
@RequiredArgsConstructor
public class TftApiService {

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
}
