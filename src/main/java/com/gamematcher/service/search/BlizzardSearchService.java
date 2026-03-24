package com.gamematcher.service.search;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Blizzard (Battle.net) 플레이어 검색 서비스
 * Blizzard OAuth2 Client Credentials Flow 사용
 * API 키 발급: https://develop.battle.net/
 *
 * 지원 게임: Overwatch 2 (주력), Hearthstone, Diablo 등
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlizzardSearchService {

    private final RestTemplate restTemplate;

    @Value("${blizzard.client.id:}")
    private String clientId;

    @Value("${blizzard.client.secret:}")
    private String clientSecret;

    // region → API 도메인
    private static final Map<String, String> REGION_DOMAIN = Map.of(
            "kr", "kr", "us", "us", "eu", "eu", "cn", "cn"
    );

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String battleTag = req.getGameName() + "#" + req.getTagLine();

        if (clientId == null || clientId.isEmpty()) {
            return PlayerSearchResponse.error("blizzard", battleTag,
                    "Blizzard API 클라이언트 ID/Secret이 설정되지 않았습니다. " +
                            "application.properties에 blizzard.client.id, blizzard.client.secret 설정 필요");
        }

        try {
            // 1) OAuth2 Access Token 획득
            String accessToken = getAccessToken(req.getRegion());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String region = REGION_DOMAIN.getOrDefault(req.getRegion(), "kr");

            // 2) 오버워치2 플레이어 프로필 (공개 API)
            //    Battle.net 개인 정보 보호 정책상 상세 전적은 제한적으로 제공됨
            String profileUrl = String.format(
                    "https://%s.api.blizzard.com/ow/profile/%s/%s",
                    region,
                    req.getGameName(),
                    req.getTagLine()
            );

            Map<String, Object> profileData = null;
            try {
                @SuppressWarnings("unchecked")
                ResponseEntity<Map> resp = restTemplate.exchange(
                        profileUrl, HttpMethod.GET, entity, Map.class);
                profileData = resp.getBody();
            } catch (Exception e) {
                log.warn("Blizzard 프로필 조회 실패 (공개 설정 필요): {}", e.getMessage());
            }

            // 프로필 공개 여부에 따라 데이터 처리
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("battleTag", battleTag);
            extras.put("note", "오버워치2는 플레이어 프로필 공개 설정이 필요합니다.");

            String tier = "N/A";
            if (profileData != null) {
                extras.put("profileData", profileData);
                Object competitive = profileData.get("competitive");
                if (competitive instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> comp = (Map<String, Object>) competitive;
                    Object pc = comp.get("pc");
                    if (pc instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> pcData = (Map<String, Object>) pc;
                        tier = String.valueOf(pcData.getOrDefault("tier", "N/A"));
                    }
                }
            }

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .gameName(req.getGameName())
                    .tagLine(req.getTagLine())
                    .tier(tier)
                    .rawData(extras)
                    .build();

            return PlayerSearchResponse.builder()
                    .success(profileData != null)
                    .game("blizzard")
                    .nickname(battleTag)
                    .playerInfo(playerInfo)
                    .matches(List.of())
                    .stats(MatchStats.builder().build())
                    .errorMessage(profileData == null
                            ? "프로필이 비공개이거나 존재하지 않는 배틀태그입니다. Battle.net 앱에서 프로필 공개 설정을 확인하세요."
                            : null)
                    .build();

        } catch (Exception e) {
            log.error("Blizzard 전적 검색 오류 - {}", battleTag, e);
            return PlayerSearchResponse.error("blizzard", battleTag, e.getMessage());
        }
    }

    /**
     * OAuth2 Client Credentials 방식으로 Access Token 획득
     */
    private String getAccessToken(String region) {
        String tokenUrl = "us".equals(region)
                ? "https://us.battle.net/oauth/token"
                : "https://kr.battle.net/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.postForObject(tokenUrl, request, Map.class);

        if (resp == null || resp.get("access_token") == null) {
            throw new RuntimeException("Blizzard Access Token 획득 실패");
        }
        return (String) resp.get("access_token");
    }

    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
