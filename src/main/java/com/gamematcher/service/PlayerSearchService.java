package com.gamematcher.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.service.search.ApexSearchService;
import com.gamematcher.service.search.Cs2SearchService;
import com.gamematcher.service.search.LolSearchService;
import com.gamematcher.service.search.OverwatchSearchService;
import com.gamematcher.service.search.PubgSearchService;
import com.gamematcher.service.search.TftSearchService;
import com.gamematcher.service.valorant.ValorantApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerSearchService {

    private final LolSearchService lolSearchService;
    private final TftSearchService tftSearchService;
    private final ValorantApiService valorantApiService;
    private final PubgSearchService pubgSearchService;
    private final OverwatchSearchService overwatchSearchService;
    private final ApexSearchService apexSearchService;
    private final Cs2SearchService cs2SearchService;
    private final ObjectMapper objectMapper;

    public PlayerSearchResponse searchPlayer(PlayerSearchRequest request) {
        request.normalize();

        String game = request.getGame();
        if (game == null || game.isBlank()) {
            return PlayerSearchResponse.error("unknown", "unknown", "검색할 게임을 선택해 주세요.");
        }

        return switch (game) {
            case "lol" -> lolSearchService.search(request);
            case "tft" -> tftSearchService.search(request);
            case "valorant" -> valorantApiService.search(request);
            case "pubg" -> pubgSearchService.search(request);
            case "overwatch" -> overwatchSearchService.search(request);
            case "apex" -> apexSearchService.search(request);
            case "cs2" -> cs2SearchService.search(request);
            default -> PlayerSearchResponse.error(game, request.getGameName(), "아직 지원하지 않는 게임입니다.");
        };
    }

    /**
     * 업로드된 JSON 배열 파일로 배치 전적 검색
     */
    public List<PlayerSearchResponse> batchSearchFromJson(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "JSON 파일을 선택해 주세요.");
        }
        try {
            List<PlayerSearchRequest> requests = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<PlayerSearchRequest>>() {});
            return batchSearch(requests);
        } catch (IOException e) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "JSON 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }

    /**
     * 요청 DTO 목록으로 배치 전적 검색
     */
    public List<PlayerSearchResponse> batchSearch(List<PlayerSearchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream().map(this::searchPlayer).toList();
    }
}
