package com.gamematcher.controller.account;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.service.apex.ApexApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Apex Legends 전적 API
 *
 * POST /api/apex/search
 * {
 *   "gameName": "Shroud",   ← Origin/EA 닉네임
 *   "platform": "PC",       ← PC | PS4 | X1
 *   "count": 5
 * }
 */
@RestController
@RequestMapping("/api/apex")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApexController {

    private final ApexApiService apexApiService;

    /**
     * Apex Legends 전적 검색
     */
    @PostMapping("/search")
    public ResponseEntity<PlayerSearchResponse> searchApex(
            @RequestBody PlayerSearchRequest request) {
        request.setGame("apex");
        return ResponseEntity.ok(apexApiService.search(request));
    }
}
