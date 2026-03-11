package com.gamematcher.controller.account;

import com.gamematcher.dto.account.ValorantSyncRequestDto;
import com.gamematcher.dto.riot.RiotAccountResponseDto;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.service.riot.RiotApiService;
import com.gamematcher.service.valorant.ValorantApiService;
import com.gamematcher.service.valorant.ValorantMatchJsonService;
import com.gamematcher.service.valorant.ValorantMatchJsonService.ValorantMatchJsonParseException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/valorant")
public class ValorantController {

    private final RiotApiService riotApiService;
    private final ValorantApiService valorantApiService;
    private final ValorantMatchJsonService valorantMatchJsonService;

    public ValorantController(RiotApiService riotApiService, ValorantApiService valorantApiService,
                             ValorantMatchJsonService valorantMatchJsonService) {
        this.riotApiService = riotApiService;
        this.valorantApiService = valorantApiService;
        this.valorantMatchJsonService = valorantMatchJsonService;
    }

    /** Valorant 최근 5경기 DB 동기화 (gameName + tagLine → puuid 조회 후 Henrik API) */
    @PostMapping("/sync")
    public String syncValorantMatches(@Valid @RequestBody ValorantSyncRequestDto request) {
        RiotAccountResponseDto account = riotApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        valorantApiService.syncValorantRecentMatches(
                account.getPuuid(),
                request.getRegion(),
                5
        );
        return "Valorant 최근 경기 DB 저장 완료";
    }

    /**
     * Valorant 전적 JSON을 DTO로 매핑
     * - API 응답 형식: { "status": 200, "data": [...] } → ValorantMatchApiResponse 반환
     * - 단일 매치: { "metadata", "players", ... } → ValorantMatchDetailDto 반환
     */
    @PostMapping("/matches/parse")
    public ResponseEntity<?> parseMatchJson(@RequestBody String json) {
        try {
            ValorantMatchApiResponse apiResponse = valorantMatchJsonService.parseApiResponse(json);
            if (apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                return ResponseEntity.ok(apiResponse);
            }
            ValorantMatchDetailDto match = valorantMatchJsonService.parseMatchDetail(json);
            return ResponseEntity.ok(match);
        } catch (ValorantMatchJsonParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
