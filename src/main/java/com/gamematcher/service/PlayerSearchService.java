package com.gamematcher.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.service.lol.LolApiService;
import com.gamematcher.service.search.Cs2SearchService;
import com.gamematcher.service.search.OverwatchSearchService;
import com.gamematcher.service.search.PubgSearchService;
import com.gamematcher.service.search.TftSearchService;
import com.gamematcher.service.search.ValorantSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerSearchService {

    private final LolApiService lolApiService;
    private final TftSearchService tftSearchService;
    private final ValorantSearchService valorantSearchService;
    private final PubgSearchService pubgSearchService;
    private final OverwatchSearchService overwatchSearchService;
    private final Cs2SearchService cs2SearchService;
    private final ObjectMapper objectMapper;

    public PlayerSearchResponse searchPlayer(PlayerSearchRequest request) {
        request.normalize();
        request.setCount(20);

        String game = request.getGame();
        if (game == null || game.isBlank()) {
            return PlayerSearchResponse.error("unknown", "unknown", "검색할 게임을 선택해 주세요.");
        }

        return switch (game) {
            case "lol" -> lolApiService.search(request);
            case "tft" -> tftSearchService.search(request);
            case "valorant" -> valorantSearchService.search(request);
            case "pubg" -> pubgSearchService.search(request);
            case "overwatch" -> overwatchSearchService.search(request);
            case "cs2" -> cs2SearchService.search(request);
            default -> PlayerSearchResponse.error(game, request.getGameName(), "아직 지원하지 않는 게임입니다.");
        };
    }

    public List<PlayerSearchResponse> batchSearchFromJson(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "JSON 파일을 선택해 주세요.");
        }
        try {
            List<PlayerSearchRequest> requests = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<PlayerSearchRequest>>() {}
            );
            return batchSearch(requests);
        } catch (IOException exception) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "JSON 형식이 올바르지 않습니다: " + exception.getMessage());
        }
    }

    public List<PlayerSearchResponse> batchSearch(List<PlayerSearchRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream().map(this::searchPlayer).toList();
    }
}
