package com.gamematcher.controller;

import com.gamematcher.dto.recruit.RecruitPostDto;
import com.gamematcher.service.RecruitPostService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recruit")
@RequiredArgsConstructor
public class RecruitController {

    private static final String SESSION_USER_ID = "userId";

    private final RecruitPostService recruitPostService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String mainPosition,
            @RequestParam(required = false) String findPosition,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String mode) {
        boolean useFilters = (tier != null && !tier.isBlank()) || (mainPosition != null && !mainPosition.isBlank())
                || (findPosition != null && !findPosition.isBlank()) || (region != null && !region.isBlank())
                || (mode != null && !mode.isBlank());
        String gameParam = game != null && !game.isBlank() ? game : "ALL";
        List<RecruitPostDto> list = useFilters
                ? recruitPostService.listWithFilters(gameParam, tier, mainPosition, findPosition, region, mode)
                : recruitPostService.listByGame(gameParam);
        return ResponseEntity.ok(Map.of("list", list));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        String summonerName = body != null ? body.get("summonerName") : null;
        if (summonerName == null || summonerName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "소환사 이름을 입력해 주세요."));
        }
        RecruitPostDto created = recruitPostService.create(
                userId,
                body.get("game"),
                summonerName,
                body.get("mainPosition"),
                body.get("findPosition"),
                body.get("tier"),
                body.get("region"),
                body.get("mode"),
                body.get("memo")
        );
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "등록에 실패했습니다."));
        }
        return ResponseEntity.ok(created);
    }
}
