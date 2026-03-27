package com.gamematcher.controller.account;

import com.gamematcher.dto.account.ValorantSyncRequestDto;
import com.gamematcher.dto.valorant.ValorantAiEvaluationResponseDto;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.service.valorant.ValorantAiEvaluationService;
import com.gamematcher.service.valorant.ValorantApiService;
import com.gamematcher.service.valorant.ValorantMatchJsonService;
import com.gamematcher.service.valorant.ValorantMatchService;
import com.gamematcher.service.valorant.ValorantMatchJsonService.ValorantMatchJsonParseException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/valorant")
public class ValorantController {

    private final ValorantApiService valorantApiService;
    private final ValorantMatchJsonService valorantMatchJsonService;
    private final ValorantMatchService valorantMatchService;
    private final ValorantAiEvaluationService valorantAiEvaluationService;

    public ValorantController(ValorantApiService valorantApiService,
                             ValorantMatchJsonService valorantMatchJsonService,
                             ValorantMatchService valorantMatchService,
                             ValorantAiEvaluationService valorantAiEvaluationService) {
        this.valorantApiService = valorantApiService;
        this.valorantMatchJsonService = valorantMatchJsonService;
        this.valorantMatchService = valorantMatchService;
        this.valorantAiEvaluationService = valorantAiEvaluationService;
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

    /**
     * 발로란트 매치 AI 평가 실행 및 저장.
     * {@code puuid} 또는 {@code gameName}/{@code tagLine}, 또는 {@code riotId}({@code 닉#태그})로 한 명만 평가 가능.
     * 필터가 없으면 매치 전원.
     */
    @PostMapping("/evaluations/match/{matchId}")
    public ResponseEntity<List<ValorantAiEvaluationResponseDto>> evaluateMatch(
            @PathVariable String matchId,
            @RequestParam(required = false) String puuid,
            @RequestParam(required = false) String gameName,
            @RequestParam(required = false) String tagLine,
            @RequestParam(required = false) String riotId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false, defaultValue = "false") boolean force
    ) {
        String gn = gameName;
        String tg = tagLine;
        if (riotId != null && !riotId.isBlank()) {
            String t = riotId.trim();
            int hash = t.lastIndexOf('#');
            if (hash > 0 && hash < t.length() - 1) {
                gn = t.substring(0, hash).trim();
                tg = t.substring(hash + 1).trim();
            } else {
                gn = t;
                tg = null;
            }
        }
        var results = valorantAiEvaluationService.evaluateAndSaveByMatchId(
                matchId, puuid, gn, tg, model, force);
        return ResponseEntity.ok(results);
    }

    /**
     * 이미 DB에 저장된 AI 평가만 조회(LLM·매치 API 미호출).
     * 전적 화면에서 모델만 바꿀 때 상세 전체를 다시 받지 않도록 사용한다.
     */
    @GetMapping("/evaluations/match/{matchId}/saved")
    public ResponseEntity<ValorantAiEvaluationResponseDto> getSavedEvaluation(
            @PathVariable String matchId,
            @RequestParam String puuid,
            @RequestParam(required = false) String model) {
        return valorantAiEvaluationService.findSavedEvaluation(matchId, puuid, model)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 발로란트 매치 플레이어 단일 AI 평가 실행 및 저장
     * - valorantMatchPlayerId: valorant_match_player 테이블의 id
     */
    @PostMapping("/evaluations/player/{valorantMatchPlayerId}")
    public ResponseEntity<ValorantAiEvaluationResponseDto> evaluatePlayer(
            @PathVariable Long valorantMatchPlayerId) {
        return valorantAiEvaluationService.evaluateAndSaveByPlayerId(valorantMatchPlayerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 발로란트 AI 평가 결과 DTO를 DB에 저장
     * - matchId, playerPuuid로 ValorantMatchPlayer 조회 후 저장 (기존 있으면 업데이트)
     */
    @PostMapping("/evaluations/save")
    public ResponseEntity<ValorantAiEvaluationResponseDto> saveEvaluation(
            @Valid @RequestBody ValorantAiEvaluationResponseDto dto) {
        return valorantAiEvaluationService.saveFromDto(dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }
}
