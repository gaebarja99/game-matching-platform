package com.gamematcher.controller;

import com.gamematcher.constant.LolPosition;
import com.gamematcher.constant.LolTier;
import com.gamematcher.exception.AlreadyInMatchingQueueException;
import com.gamematcher.service.LolMatchingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * LoL 5인 랜덤 매칭 API.
 * <p>
 * <strong>실시간 알림</strong>: 매칭이 완료되면 기존 STOMP와 동일하게
 * {@code /topic/user/{userId}} 로 {@code type=MATCH_COMPLETE_LOL} 페이로드가 전송됩니다.
 * (프론트는 기존 WebSocket 구독을 재사용하면 됩니다.)
 * </p>
 * <p>
 * <strong>폴링 대안</strong>: {@code GET /api/lol-match/queue/status} 로 대기 여부·티어·포지션을 주기적으로 확인할 수 있습니다.
 * </p>
 */
@RestController
@RequestMapping("/api/lol-match")
@RequiredArgsConstructor
public class LolMatchingController {

    private static final String SESSION_USER_ID = "userId";

    private final LolMatchingService lolMatchingService;

    @PostMapping("/queue/join")
    public ResponseEntity<?> joinQueue(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        String tierRaw = body != null ? body.get("tier") : null;
        String posRaw = body != null ? body.get("position") : null;
        if (tierRaw == null || tierRaw.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "tier(티어)를 입력해 주세요."));
        }
        LolTier tier;
        try {
            tier = LolTier.valueOf(tierRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 티어입니다."));
        }
        LolPosition position = LolPosition.fromClient(posRaw);
        try {
            lolMatchingService.enqueue(userId, tier, position);
            return ResponseEntity.ok(Map.of("inQueue", true, "gameName", LolMatchingService.GAME_LOL));
        } catch (AlreadyInMatchingQueueException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/queue/leave")
    public ResponseEntity<?> leaveQueue(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean left = lolMatchingService.leaveQueue(userId);
        return ResponseEntity.ok(Map.of("left", left, "inQueue", !left));
    }

    @GetMapping("/queue/status")
    public ResponseEntity<?> queueStatus(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(lolMatchingService.queueStatus(userId));
    }
}
