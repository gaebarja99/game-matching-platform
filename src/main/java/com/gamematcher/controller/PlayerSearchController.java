package com.gamematcher.controller;

import com.gamematcher.dto.search.*;
import com.gamematcher.service.PlayerSearchService;
import com.gamematcher.service.search.RecordsMatchDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 닉네임 기반 통합 전적 검색 컨트롤러
 * - 단일 닉네임 검색: POST /api/search/player
 * - JSON 파일 배치 검색: POST /api/search/batch
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlayerSearchController {

    private final PlayerSearchService playerSearchService;
    private final RecordsMatchDetailService recordsMatchDetailService;



    /**
     * 단일 플레이어 전적 검색
     * 예시 요청:
     * {
     *   "game": "lol",          // lol | tft | valorant | steam | blizzard
     *   "gameName": "hide on bush",
     *   "tagLine": "KR1",       // Riot 계열만 필요 (lol, tft, valorant)
     *   "region": "kr",         // 선택 (기본값: kr)
     *   "count": 5,             // 조회할 매치 수 (기본값: 5, 최대: 20)
     *   "forceRefresh": false  // true면 LoL/TFT/발로/PUBG 매치 DB 캐시 무시 후 API 갱신
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
     * JSON 파일 업로드 배치 전적 검색
     * JSON 형식:
     * [
     *   { "game": "lol", "gameName": "hide on bush", "tagLine": "KR1" },
     *   { "game": "valorant", "gameName": "TenZ", "tagLine": "SEN" },
     *   { "game": "steam", "steamId": "76561198000000000" }
     * ]
     */
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
