package com.gamematcher.service.steam;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Steam 플레이어 검색 서비스
 * Steam Web API 사용 - https://steamcommunity.com/dev
 * API 키 발급: https://steamcommunity.com/dev/apikey
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SteamApiService {

    private final RestTemplate restTemplate;

    @Value("${steam.api.key:}")
    private String steamApiKey;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        // Steam은 steamId 또는 gameName(vanity URL) 사용
        String identifier = req.getSteamId() != null ? req.getSteamId() : req.getGameName();

        if (steamApiKey == null || steamApiKey.isEmpty()) {
            return PlayerSearchResponse.error("steam", identifier,
                    "Steam API 키가 설정되지 않았습니다. application.properties에 steam.api.key 설정 필요");
        }

        try {
            String steam64Id = resolveSteamId(identifier);

            // 1) 플레이어 요약 정보
            String profileUrl = String.format(
                    "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=%s&steamids=%s",
                    steamApiKey, steam64Id
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> profileResp = restTemplate.getForObject(profileUrl, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = profileResp != null
                    ? (Map<String, Object>) profileResp.get("response") : null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> players = response != null
                    ? (List<Map<String, Object>>) response.get("players") : null;
            if (players == null || players.isEmpty()) throw new RuntimeException("Steam 플레이어를 찾을 수 없습니다.");

            Map<String, Object> player = players.get(0);
            String displayName = (String) player.getOrDefault("personaname", identifier);
            String avatarUrl   = (String) player.get("avatarmedium");

            // 2) 최근 플레이 게임 목록
            String gamesUrl = String.format(
                    "https://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/?key=%s&steamid=%s&count=%d",
                    steamApiKey, steam64Id, Math.min(req.getCount(), 20)
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> gamesResp = restTemplate.getForObject(gamesUrl, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> gamesResponse = gamesResp != null
                    ? (Map<String, Object>) gamesResp.get("response") : null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> games = gamesResponse != null
                    ? (List<Map<String, Object>>) gamesResponse.get("games") : List.of();

            // 3) 보유 게임 총 수
            String ownedUrl = String.format(
                    "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=%s&steamid=%s&include_appinfo=false",
                    steamApiKey, steam64Id
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> ownedResp = restTemplate.getForObject(ownedUrl, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> ownedResponse = ownedResp != null
                    ? (Map<String, Object>) ownedResp.get("response") : null;
            int totalGames = ownedResponse != null
                    ? toInt(ownedResponse.get("game_count")) : 0;

            // MatchInfo로 게임 목록 변환 (전적 대신 플레이 기록)
            List<MatchInfo> matches = new ArrayList<>();
            if (games != null) {
                for (Map<String, Object> game : games) {
                    int appId     = toInt(game.get("appid"));
                    String name   = (String) game.getOrDefault("name", "App " + appId);
                    int playtime2weeks = toInt(game.get("playtime_2weeks")); // 분
                    int playtimeForever = toInt(game.get("playtime_forever")); // 분

                    Map<String, Object> extras = new LinkedHashMap<>();
                    extras.put("appId",           appId);
                    extras.put("playtime2weeks_hr", String.format("%.1f시간", playtime2weeks / 60.0));
                    extras.put("playtimeTotal_hr",  String.format("%.1f시간", playtimeForever / 60.0));
                    extras.put("storeUrl", "https://store.steampowered.com/app/" + appId);

                    matches.add(MatchInfo.builder()
                            .matchId(String.valueOf(appId))
                            .gameMode(name)
                            .playtime(playtime2weeks * 60) // 초로 변환
                            .extras(extras)
                            .build());
                }
            }

            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("totalOwnedGames", totalGames);
            rawData.put("steam64Id", steam64Id);

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .steamId(steam64Id)
                    .gameName(displayName)
                    .avatarUrl(avatarUrl)
                    .rawData(rawData)
                    .build();

            MatchStats stats = MatchStats.builder()
                    .totalGames(matches.size())
                    .build();

            return PlayerSearchResponse.builder()
                    .success(true)
                    .game("steam")
                    .nickname(displayName)
                    .playerInfo(playerInfo)
                    .matches(matches)
                    .stats(stats)
                    .build();

        } catch (Exception e) {
            log.error("Steam 전적 검색 오류 - {}", identifier, e);
            return PlayerSearchResponse.error("steam", identifier, e.getMessage());
        }
    }

    /**
     * vanity URL → Steam64 ID 변환 (숫자만이면 그대로 사용)
     */
    private String resolveSteamId(String identifier) {
        if (identifier != null && identifier.matches("\\d{17}")) {
            return identifier; // 이미 Steam64 ID
        }
        String resolveUrl = String.format(
                "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=%s&vanityurl=%s",
                steamApiKey, identifier
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(resolveUrl, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = resp != null ? (Map<String, Object>) resp.get("response") : null;
        if (response != null && Integer.valueOf(1).equals(response.get("success"))) {
            return (String) response.get("steamid");
        }
        throw new RuntimeException("Steam ID를 찾을 수 없습니다: " + identifier);
    }

    private int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }
}

