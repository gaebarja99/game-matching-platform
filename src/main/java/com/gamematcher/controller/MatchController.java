package com.gamematcher.controller;

import com.gamematcher.entity.MatchChatMessage;
import com.gamematcher.service.MatchService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private static final String SESSION_USER_ID = "userId";

    private final MatchService matchService;

    /** 랜덤 매칭 대기열 참가 */
    @PostMapping("/queue/join")
    public ResponseEntity<?> joinQueue(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String game = body != null ? body.get("game") : null;
        if (game == null || game.isBlank()) game = "LEAGUE_OF_LEGENDS";
        try {
            Map<String, Object> result = matchService.joinQueue(userId, game,
                    body != null ? body.get("tier") : null,
                    body != null ? body.get("position") : null);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 대기열 나가기 */
    @DeleteMapping("/queue/leave")
    public ResponseEntity<?> leaveQueue(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        boolean ok = matchService.leaveQueue(userId);
        return ResponseEntity.ok(Map.of("inQueue", !ok));
    }

    /** 대기열 상태 */
    @GetMapping("/queue/status")
    public ResponseEntity<?> queueStatus(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        return ResponseEntity.ok(Map.of("inQueue", matchService.isInQueue(userId)));
    }

    /** 매칭 세션 조회 (매칭 완료 시 모달용) */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        return matchService.getSession(sessionId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("message", "매칭 세션을 찾을 수 없습니다.")));
    }

    /** 내 랜덤 매칭 내역 (채팅 다시 보기용) */
    @GetMapping("/sessions")
    public ResponseEntity<?> mySessions(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        List<Map<String, Object>> list = matchService.getMySessions(userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 매칭 채팅 메시지 목록 */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "50") int limit,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        List<Map<String, Object>> list = matchService.getMatchChatMessages(sessionId, userId, limit);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 매칭 채팅 전송 */
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String text = body != null && body.get("text") != null ? body.get("text").toString() : null;
        try {
            MatchChatMessage msg = matchService.sendMatchChat(sessionId, userId, text);
            return ResponseEntity.ok(Map.of("id", msg.getId(), "text", msg.getText(), "createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : ""));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 랜덤 매칭 세션 내역 삭제 (참가자만) */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable Long sessionId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        boolean ok = matchService.deleteSession(sessionId, userId);
        if (!ok) return ResponseEntity.status(404).body(Map.of("message", "세션을 찾을 수 없거나 삭제 권한이 없습니다."));
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
