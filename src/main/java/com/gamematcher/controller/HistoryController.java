package com.gamematcher.controller;

import com.gamematcher.service.HistoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private static final String SESSION_USER_ID = "userId";

    private final HistoryService historyService;

    /** 시청 기록 조회: 어떤 스트리머를 몇 시간/분 시청했는지 */
    @GetMapping("/watch")
    public ResponseEntity<?> watchHistory(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<Map<String, Object>> list = historyService.getWatchHistory(userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 방송 기록 조회: 내가 방송한 시간 */
    @GetMapping("/broadcast")
    public ResponseEntity<?> broadcastHistory(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<Map<String, Object>> list = historyService.getBroadcastHistory(userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 방송별 받은 팡 */
    @GetMapping("/pang")
    public ResponseEntity<?> pangByStream(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<Map<String, Object>> list = historyService.getPangByStream(userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 시청 시간 기록 (watch 페이지에서 주기적으로 또는 이탈 시 호출) */
    @PostMapping("/watch")
    public ResponseEntity<?> recordWatch(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Long streamId = null;
        Object sid = body.get("streamId");
        if (sid instanceof Number) streamId = ((Number) sid).longValue();
        else if (sid != null) try { streamId = Long.parseLong(sid.toString()); } catch (NumberFormatException ignored) {}
        if (streamId == null || streamId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "방송 번호가 필요합니다."));
        }
        long addSeconds = 0;
        Object sec = body.get("seconds");
        if (sec instanceof Number) addSeconds = ((Number) sec).longValue();
        else if (sec != null) try { addSeconds = Long.parseLong(sec.toString()); } catch (NumberFormatException ignored) {}
        if (addSeconds <= 0) {
            return ResponseEntity.ok(Map.of("ok", true));
        }
        historyService.addWatchTime(userId, streamId, addSeconds);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
