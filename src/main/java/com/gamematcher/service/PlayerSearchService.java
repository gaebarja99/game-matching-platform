package com.gamematcher.service;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.service.search.Cs2SearchService;
import com.gamematcher.service.search.LolSearchService;
import com.gamematcher.service.search.OverwatchSearchService;
import com.gamematcher.service.search.PubgSearchService;
import com.gamematcher.service.search.TftSearchService;
import com.gamematcher.service.search.ValorantSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerSearchService {

    private final LolSearchService lolSearchService;
    private final TftSearchService tftSearchService;
    private final ValorantSearchService valorantSearchService;
    private final PubgSearchService pubgSearchService;
    private final OverwatchSearchService overwatchSearchService;
    private final Cs2SearchService cs2SearchService;

    public PlayerSearchResponse searchPlayer(PlayerSearchRequest request) {
        request.normalize();
        request.setCount(20);

        String game = request.getGame();
        if (game == null || game.isBlank()) {
            return PlayerSearchResponse.error("unknown", "unknown", "검색할 게임을 선택해 주세요.");
        }

        return switch (game) {
            case "lol" -> lolSearchService.search(request);
            case "tft" -> tftSearchService.search(request);
            case "valorant" -> valorantSearchService.search(request);
            case "pubg" -> pubgSearchService.search(request);
            case "overwatch" -> overwatchSearchService.search(request);
            case "cs2" -> cs2SearchService.search(request);
            default -> PlayerSearchResponse.error(game, request.getGameName(), "아직 지원하지 않는 게임입니다.");
        };
    }
}
