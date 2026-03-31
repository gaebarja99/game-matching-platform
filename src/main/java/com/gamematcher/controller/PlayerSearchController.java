package com.gamematcher.controller;

import com.gamematcher.dto.search.*;
import com.gamematcher.service.PlayerSearchService;
import com.gamematcher.service.search.RecordsAiEvaluationService;
import com.gamematcher.service.search.RecordsMatchDetailService;
import com.gamematcher.service.valorant.ValorantApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 닉네임 기반 통합 전적 검색 컨트롤러
 * - 단일 닉네임 검색: POST /api/search/player
 * - JSON 파일 배치 검색: POST /api/search/batch
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class PlayerSearchController {

    private final PlayerSearchService playerSearchService;
    private final RecordsMatchDetailService recordsMatchDetailService;
    private final RecordsAiEvaluationService recordsAiEvaluationService;
    private final ValorantApiService valorantApiService;



    /**
     * 단일 플레이어 전적 검색
     * 예시 요청:
     * {
     *   "game": "lol",          // lol | tft | valorant | steam | blizzard
     *   "gameName": "hide on bush",
     *   "tagLine": "KR1",       // Riot 계열만 필요 (lol, tft, valorant)
     *   "region": "kr",         // 선택 (기본값: kr)
     *   "count": 10,            // 조회할 매치 수 (기본값: 10, 최대: 20)
     *   "forceRefresh": false, // true면 LoL/TFT/발로/PUBG 매치 DB 캐시 무시 후 API 갱신
     *   "matchListOnly": false, // true면 LoL/TFT는 매치 ID만(상세는 POST /api/search/match-detail)
     *   "deferValorantMmr": false, // true면 발로란트는 매치·통계만 먼저, 티어는 POST /api/search/valorant/mmr
     *   "accountOnly": false,      // true면 계정 존재만(발로: Henrik account만). 이후 valorantPrefetchPuuid 등으로 전체 검색
     *   "valorantPrefetchPuuid": "", "valorantPrefetchAccountRegion": "", "valorantPrefetchCardUrl": ""
     * }
     */
    @PostMapping("/player")
    public ResponseEntity<PlayerSearchResponse> searchPlayer(
            @RequestBody PlayerSearchRequest request) {
        log.info("전적 검색 요청 - game: {}, gameName: {}, tagLine: {}",
                request.getGame(), request.getGameName(), request.getTagLine());
        PlayerSearchResponse response = playerSearchService.searchPlayer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 매치 상세(전체 JSON) 지연 로드 — Records 화면에서 행 펼칠 때 호출
     */
    @PostMapping("/match-detail")
    public ResponseEntity<MatchDetailResponse> matchDetail(@RequestBody MatchDetailRequest request) {
        log.info("매치 상세 요청 - game: {}, matchId: {}", request.getGame(), request.getMatchId());
        return ResponseEntity.ok(recordsMatchDetailService.load(request));
    }

    /**
     * 저장된 AI 평가만 DB 조회 (외부 매치 API 없음). 전적 상세에서 모델·게임만 바꿀 때 사용.
     * {@code game}: valorant | lol | pubg
     */
    @GetMapping("/saved-ai-evaluation")
    public ResponseEntity<Map<String, Object>> savedAiEvaluation(
            @RequestParam String game,
            @RequestParam String matchId,
            @RequestParam(required = false) String puuid,
            @RequestParam(required = false) String playerName,
            @RequestParam(required = false) String llmModel) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        String g = game != null ? game.trim().toLowerCase(Locale.ROOT) : "";
        Optional<Map<String, Object>> ai = switch (g) {
            case "valorant" -> recordsMatchDetailService.loadValorantSavedAiEvaluationOnly(matchId, puuid, llmModel);
            case "lol" -> recordsMatchDetailService.loadLolSavedAiEvaluationOnly(matchId, puuid, llmModel);
            case "pubg" -> recordsMatchDetailService.loadPubgSavedAiEvaluationOnly(matchId, playerName);
            default -> Optional.empty();
        };
        ai.ifPresent(m -> body.put("records_ai_evaluation", m));
        return ResponseEntity.ok(body);
    }

    /**
     * 발로란트 전적 1차 응답({@code deferValorantMmr}) 후 티어만 보강.
     */
    @PostMapping("/valorant/mmr")
    public ResponseEntity<ValorantSearchMmrResponse> valorantSearchMmr(@RequestBody ValorantSearchMmrRequest request) {
        request.normalize();
        log.info("발로란트 MMR 지연 로드 - region: {}", request.getRegion());
        return ResponseEntity.ok(valorantApiService.resolveMmrForSearch(request));
    }

    /**
     * JSON 파일 업로드 배치 전적 검색
     * JSON 형식:
     * [
     *   { "game": "lol", "gameName": "hide on bush", "tagLine": "KR1" },
     *   { "game": "valorant", "gameName": "TenZ", "tagLine": "SEN" },
     *   { "game": "steam", "steamId": "76561198000000000" }
     * ]
     */
    @PostMapping("/lol/evaluations/match/{matchId}")
    public ResponseEntity<RecordsAiEvaluationResponse> evaluateLolMatch(
            @PathVariable String matchId,
            @RequestParam String puuid,
            @RequestParam(required = false) String model,
            @RequestParam(defaultValue = "40") int maxTimelineEvents) {
        return ResponseEntity.ok(
                recordsAiEvaluationService.evaluateLolMatch(matchId, puuid, model, maxTimelineEvents)
        );
    }

    @PostMapping("/batch")
    public ResponseEntity<List<PlayerSearchResponse>> batchSearch(
            @RequestParam("file") MultipartFile file) {
        log.info("배치 전적 검색 요청 - 파일명: {}, 크기: {}bytes",
                file.getOriginalFilename(), file.getSize());
        List<PlayerSearchResponse> responses = playerSearchService.batchSearchFromJson(file);
        return ResponseEntity.ok(responses);
    }

    /**
     * JSON 본문으로 배치 전적 검색 (파일 없이 직접 JSON 배열 전송)
     */
    @PostMapping("/batch/json")
    public ResponseEntity<List<PlayerSearchResponse>> batchSearchJson(
            @RequestBody List<PlayerSearchRequest> requests) {
        log.info("JSON 배치 전적 검색 요청 - {}명", requests.size());
        List<PlayerSearchResponse> responses = playerSearchService.batchSearch(requests);
        return ResponseEntity.ok(responses);
    }
}
