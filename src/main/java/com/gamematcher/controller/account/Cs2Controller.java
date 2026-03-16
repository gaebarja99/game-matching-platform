package com.gamematcher.controller.account;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.service.cs2.Cs2ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Counter-Strike 2 전적 API
 *
 * POST /api/cs2/search
 * {
 *   "gameName": "76561198000000000",  ← Steam64 ID 또는 Vanity URL
 *   "count": 5
 * }
 */
@RestController
@RequestMapping("/api/cs2")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class Cs2Controller {

    private final Cs2ApiService cs2ApiService;

    /**
     * CS2 전적 검색
     * Steam64 ID 또는 Vanity URL로 조회
     */
    @PostMapping("/search")
    public ResponseEntity<PlayerSearchResponse> searchCs2(
            @RequestBody PlayerSearchRequest request) {
        request.setGame("cs2");
        return ResponseEntity.ok(cs2ApiService.search(request));
    }
}
