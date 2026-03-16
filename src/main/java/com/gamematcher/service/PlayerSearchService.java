package com.gamematcher.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.service.apex.ApexApiService;
import com.gamematcher.service.blizzard.BlizzardApiService;
import com.gamematcher.service.cs2.Cs2ApiService;
import com.gamematcher.service.lol.LolApiService;
import com.gamematcher.service.overwatch.OverwatchApiService;
import com.gamematcher.service.pubg.PubgApiService;
import com.gamematcher.service.steam.SteamApiService;
import com.gamematcher.service.tft.TftApiService;
import com.gamematcher.service.valorant.ValorantApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 통합 전적 검색 서비스
 * 게임 종류에 따라 각 전문 서비스로 라우팅
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerSearchService {

    private final LolApiService        lolApiService;
    private final TftApiService        tftApiService;
    private final ValorantApiService   valorantApiService;
    private final SteamApiService      steamApiService;
    private final BlizzardApiService   blizzardApiService;
    private final PubgApiService       pubgApiService;
    private final OverwatchApiService  overwatchApiService;
    private final Cs2ApiService        cs2ApiService;
    private final ApexApiService       apexApiService;
    private final ObjectMapper            objectMapper;

    /**
     * 단일 플레이어 전적 검색
     */
    public PlayerSearchResponse searchPlayer(PlayerSearchRequest request) {
        request.normalize();

        String nickname = buildNickname(request);

        try {
            return switch (request.getGame()) {
                case "lol"        -> lolApiService.search(request);
                case "tft"        -> tftApiService.search(request);
                case "valorant"   -> valorantApiService.search(request);
                case "steam"      -> steamApiService.search(request);
                case "blizzard"   -> blizzardApiService.search(request);
                case "pubg"       -> pubgApiService.search(request);
                case "overwatch"  -> overwatchApiService.search(request);
                case "cs2"        -> cs2ApiService.search(request);
                case "apex"       -> apexApiService.search(request);
                default           -> PlayerSearchResponse.error(
                        request.getGame(), nickname,
                        "지원하지 않는 게임입니다: " + request.getGame()
                                + " (지원: lol, tft, valorant, steam, blizzard, pubg, overwatch, cs2, apex)");
            };
        } catch (Exception e) {
            log.error("전적 검색 오류 - game: {}, nickname: {}", request.getGame(), nickname, e);
            return PlayerSearchResponse.error(request.getGame(), nickname,
                    "전적 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * JSON 파일 업로드로 배치 검색
     */
    public List<PlayerSearchResponse> batchSearchFromJson(MultipartFile file) {
        List<PlayerSearchRequest> requests;
        try {
            requests = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<PlayerSearchRequest>>() {}
            );
        } catch (IOException e) {
            log.error("JSON 파일 파싱 오류", e);
            return List.of(PlayerSearchResponse.error(
                    "unknown", "unknown",
                    "JSON 파일 파싱 오류: " + e.getMessage()
                            + " | 형식: [{\"game\":\"lol\",\"gameName\":\"hide on bush\",\"tagLine\":\"KR1\"}]"
            ));
        }
        return batchSearch(requests);
    }

    /**
     * 요청 목록으로 배치 검색
     */
    public List<PlayerSearchResponse> batchSearch(List<PlayerSearchRequest> requests) {
        List<PlayerSearchResponse> results = new ArrayList<>();
        for (PlayerSearchRequest req : requests) {
            try {
                results.add(searchPlayer(req));
                // API rate limit 방지 (라이엇 기준 1초 2req)
                Thread.sleep(600);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("배치 검색 중 개별 오류", e);
                req.normalize();
                results.add(PlayerSearchResponse.error(
                        req.getGame(), buildNickname(req), e.getMessage()));
            }
        }
        return results;
    }

    private String buildNickname(PlayerSearchRequest req) {
        if (req.getGameName() != null && req.getTagLine() != null) {
            return req.getGameName() + "#" + req.getTagLine();
        }
        if (req.getGameName() != null) return req.getGameName();
        if (req.getSteamId() != null)  return req.getSteamId();
        return "unknown";
    }
}
