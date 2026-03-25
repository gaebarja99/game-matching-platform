package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.ValorantApiProperties;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
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

/**
 * Valorant 전적 조회 (HenrikDev API) 및 DB 저장
 */
@Service
@RequiredArgsConstructor
public class ValorantApiService {

    private final ValorantApiProperties valorantApiProperties;
    private final ValorantMatchService valorantMatchService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Henrik API로 name+tag에서 puuid 조회
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
            return objectMapper.readValue(response.getBody(), ValorantPuuidApiResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant 계정 API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 계정 조회 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 지정 puuid의 최근 매치를 API에서 조회하여 DB에 저장
     * @param puuid 플레이어 puuid (API 필터용)
     * @param region 리전 (ap, kr 등)
     * @param count 저장할 매치 수
     */
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
            throw new RuntimeException("Valorant API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 전적 동기화 오류: " + e.getMessage(), e);
        }
    }
}
