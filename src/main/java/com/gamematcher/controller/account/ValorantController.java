package com.gamematcher.controller.account;

import com.gamematcher.dto.account.ValorantSyncRequestDto;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.service.valorant.ValorantApiService;
import com.gamematcher.service.valorant.ValorantMatchJsonService;
import com.gamematcher.service.valorant.ValorantMatchService;
import com.gamematcher.service.valorant.ValorantMatchJsonService.ValorantMatchJsonParseException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/valorant")
public class ValorantController {

    private final ValorantApiService valorantApiService;
    private final ValorantMatchJsonService valorantMatchJsonService;
    private final ValorantMatchService valorantMatchService;

    public ValorantController(ValorantApiService valorantApiService,
                             ValorantMatchJsonService valorantMatchJsonService,
                             ValorantMatchService valorantMatchService) {
        this.valorantApiService = valorantApiService;
        this.valorantMatchJsonService = valorantMatchJsonService;
        this.valorantMatchService = valorantMatchService;
    }

    /** Valorant 최근 5경기 DB 동기화 (Henrik API로 gameName+tagLine → puuid 조회 후 매치 저장) */
    @PostMapping("/sync")
    public String syncValorantMatches(@Valid @RequestBody ValorantSyncRequestDto request) {
        var accountResponse = valorantApiService.getAccountByNameTag(
                request.getGameName(),
                request.getTagLine()
        );
        String puuid = accountResponse.getData() != null ? accountResponse.getData().getPuuid() : null;
        if (puuid == null || puuid.isBlank()) {
            throw new IllegalArgumentException("Valorant 계정을 찾을 수 없습니다: " + request.getGameName() + "#" + request.getTagLine());
        }
        String region = (accountResponse.getData() != null && accountResponse.getData().getRegion() != null)
                ? accountResponse.getData().getRegion()
                : request.getRegion();
        valorantApiService.syncValorantRecentMatches(puuid, region, 5);
        return "Valorant 최근 경기 DB 저장 완료";
    }

    /**
     * Valorant 매치 JSON을 DB에 저장
     * - 단일 매치: { "metadata", "players", ... }
     * - API 응답 형식: { "status": 200, "data": [...] } → data[0] 저장
     */
    @PostMapping("/matches/save")
    public ResponseEntity<?> saveMatchJson(@RequestBody String json) {
        try {
            ValorantMatchDetailDto matchDto = valorantMatchJsonService.parseFirstMatch(json);
            ValorantMatch saved = valorantMatchService.saveMatch(matchDto);
            if (saved != null) {
                return ResponseEntity.ok("Valorant 매치 저장 완료: " + saved.getMatchId());
            }
            String matchId = matchDto.getMetadata() != null ? matchDto.getMetadata().getMatchId() : "unknown";
            return ResponseEntity.ok("이미 존재하는 매치입니다: " + matchId);
        } catch (ValorantMatchJsonParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
