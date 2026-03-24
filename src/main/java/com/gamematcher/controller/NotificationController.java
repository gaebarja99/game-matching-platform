package com.gamematcher.controller;

import com.gamematcher.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final String SESSION_USER_ID = "userId";

    private final NotificationService notificationService;

    @GetMapping("/count")
    public ResponseEntity<?> unreadCount(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/dm-count")
    public ResponseEntity<?> unreadDmCount(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        long count = notificationService.getUnreadDmCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "limit", defaultValue = "30") int limit,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        if (limit > 100) limit = 100;
        List<Map<String, Object>> list = notificationService.getList(userId, limit);
        return ResponseEntity.ok(Map.of("list", list));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAll(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        notificationService.deleteAll(userId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/read-dm-from")
    public ResponseEntity<?> markDmReadFrom(
            @RequestParam("fromUserId") Long fromUserId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        int marked = notificationService.markNewDmAsReadByActor(userId, fromUserId);
        return ResponseEntity.ok(Map.of("ok", true, "marked", marked));
    }
}
