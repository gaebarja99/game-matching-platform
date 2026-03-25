package com.gamematcher.controller;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.service.PlayerSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class PlayerSearchController {

    private final PlayerSearchService playerSearchService;

    @PostMapping("/player")
    public ResponseEntity<PlayerSearchResponse> searchPlayer(@RequestBody PlayerSearchRequest request) {
        return ResponseEntity.ok(playerSearchService.searchPlayer(request));
    }
}
