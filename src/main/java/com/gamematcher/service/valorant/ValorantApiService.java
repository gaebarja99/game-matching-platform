package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.ValorantApiProperties;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.dto.valorant.ValorantPlayerDto;
import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class ValorantApiService {

    private final ValorantApiProperties valorantApiProperties;
    private final ValorantMatchService valorantMatchService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValorantPuuidApiResponse getAccountByNameTag(String name, String tag) {
        RuntimeException lastException = null;
        for (String path : new String[]{"/valorant/v2/account/{name}/{tag}", "/valorant/v1/account/{name}/{tag}"}) {
            String url = UriComponentsBuilder
                    .fromHttpUrl(valorantApiProperties.getBaseUrl())
                    .path(path)
                    .queryParam("force", true)
                    .buildAndExpand(name, tag)
                    .encode()
                    .toUriString();

            try {
                return readAccountResponse(url, "Valorant account API call failed");
            } catch (RuntimeException e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new RuntimeException("Valorant account lookup failed.");
    }

    public ValorantPuuidApiResponse getAccountByPuuid(String puuid) {
        RuntimeException lastException = null;
        for (String path : new String[]{"/valorant/v2/by-puuid/account/{puuid}", "/valorant/v1/by-puuid/account/{puuid}"}) {
            String url = UriComponentsBuilder
                    .fromHttpUrl(valorantApiProperties.getBaseUrl())
                    .path(path)
                    .queryParam("force", true)
                    .buildAndExpand(puuid)
                    .encode()
                    .toUriString();

            try {
                return readAccountResponse(url, "Valorant account-by-puuid API call failed");
            } catch (RuntimeException e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new RuntimeException("Valorant account-by-puuid lookup failed.");
    }

    private ValorantPuuidApiResponse readAccountResponse(String url, String errorPrefix) {
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
            return parseAccountResponse(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(errorPrefix + ": " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException(errorPrefix + ": " + e.getMessage(), e);
        }
    }

    private ValorantPuuidApiResponse parseAccountResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);

        ValorantPuuidApiResponse response = new ValorantPuuidApiResponse();
        response.setStatus(root.path("status").asInt(200));

        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            return response;
        }

        ValorantPuuidApiResponse.AccountData account = new ValorantPuuidApiResponse.AccountData();
        account.setPuuid(textOrNull(dataNode, "puuid"));
        account.setRegion(textOrNull(dataNode, "region"));
        account.setAccountLevel(dataNode.path("account_level").isNumber() ? dataNode.path("account_level").asInt() : null);
        account.setName(textOrNull(dataNode, "name"));
        account.setTag(textOrNull(dataNode, "tag"));
        account.setLastUpdate(textOrNull(dataNode, "last_update"));
        account.setLastUpdateRaw(dataNode.path("last_update_raw").isNumber() ? dataNode.path("last_update_raw").asLong() : null);

        JsonNode cardNode = dataNode.path("card");
        if (!cardNode.isMissingNode() && !cardNode.isNull()) {
            ValorantPuuidApiResponse.Card card = new ValorantPuuidApiResponse.Card();
            if (cardNode.isTextual()) {
                card.setId(cardNode.asText());
            } else {
                card.setId(textOrNull(cardNode, "id"));
                card.setSmall(textOrNull(cardNode, "small"));
                card.setLarge(textOrNull(cardNode, "large"));
                card.setWide(textOrNull(cardNode, "wide"));
            }
            account.setCard(card);
        }

        response.setData(account);
        return response;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    public String extractFriendlyAccountLookupError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Valorant 계정 정보를 불러오지 못했습니다.";
        }
        if (message.contains("404") || message.contains("Account not found")) {
            return "입력한 Riot ID로 Valorant 계정을 찾을 수 없습니다. 실제 발로란트 게임명과 태그를 다시 확인해 주세요.";
        }
        if (message.contains("Invalid UUID/PUUID")) {
            return "Valorant 계정 조회 경로에서 PUUID를 인식하지 못했습니다. 다른 조회 방식으로 다시 시도해 주세요.";
        }
        return "Valorant 계정 정보를 불러오지 못했습니다.";
    }

    public ValorantMatchApiResponse getRecentMatchesByPuuid(String region, String puuid, int size) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v3/by-puuid/matches/{region}/{puuid}")
                .queryParam("size", size)
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
            return objectMapper.readValue(response.getBody(), ValorantMatchApiResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant recent matches API call failed: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant recent matches lookup failed: " + e.getMessage(), e);
        }
    }

    public PlayerCardSnapshot getLatestPlayerCardFromRecentMatches(String region, String puuid) {
        ValorantMatchApiResponse response = getRecentMatchesByPuuid(region, puuid, 1);
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new RuntimeException("No recent matches returned for region=" + region + ", puuid=" + puuid);
        }

        ValorantMatchDetailDto match = response.getData().get(0);
        if (match == null) {
            throw new RuntimeException("Recent match payload is empty for region=" + region + ", puuid=" + puuid);
        }
        if (match.getPlayers() == null || match.getPlayers().getAllPlayers() == null) {
            String matchId = match.getMetadata() != null ? match.getMetadata().getMatchId() : "unknown";
            throw new RuntimeException("Recent match has no player list. matchId=" + matchId + ", region=" + region + ", puuid=" + puuid);
        }

        for (ValorantPlayerDto player : match.getPlayers().getAllPlayers()) {
            if (player != null && puuid.equals(player.getPuuid())) {
                String imageUrl = player.getAssets() != null && player.getAssets().getCard() != null
                        ? player.getAssets().getCard().getSmall()
                        : null;
                String matchId = match.getMetadata() != null ? match.getMetadata().getMatchId() : null;
                if (player.getPlayerCard() == null || player.getPlayerCard().isBlank()) {
                    throw new RuntimeException("Recent match player found but player_card is empty. matchId=" + matchId + ", puuid=" + puuid);
                }
                return new PlayerCardSnapshot(player.getPlayerCard(), imageUrl, matchId);
            }
        }

        String matchId = match.getMetadata() != null ? match.getMetadata().getMatchId() : "unknown";
        throw new RuntimeException("Recent match does not contain target player. matchId=" + matchId + ", puuid=" + puuid);
    }

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

            ValorantMatchApiResponse apiResponse = objectMapper.readValue(
                    response.getBody(),
                    ValorantMatchApiResponse.class
            );

            if (apiResponse.getData() == null || apiResponse.getData().isEmpty()) {
                return;
            }

            valorantMatchService.saveMatches(apiResponse.getData(), count);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant API call failed: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant recent match sync failed: " + e.getMessage(), e);
        }
    }

    public record PlayerCardSnapshot(String cardId, String cardImageUrl, String matchId) {
    }
}
