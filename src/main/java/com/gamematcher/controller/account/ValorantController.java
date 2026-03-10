package com.gamematcher.controller.account;

import com.gamematcher.dto.account.ValorantSyncRequestDto;
import com.gamematcher.dto.riot.RiotAccountResponseDto;
import com.gamematcher.service.riot.RiotApiService;
import com.gamematcher.service.valorant.ValorantApiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/valorant")
public class ValorantController {

    private final RiotApiService riotApiService;
    private final ValorantApiService valorantApiService;

    public ValorantController(RiotApiService riotApiService, ValorantApiService valorantApiService) {
        this.riotApiService = riotApiService;
        this.valorantApiService = valorantApiService;
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
}
