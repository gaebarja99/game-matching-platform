package com.gamematcher.controller;

import com.gamematcher.entity.DmMessage;
import com.gamematcher.service.DmService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dm")
@RequiredArgsConstructor
public class DmController {

    private static final String SESSION_USER_ID = "userId";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DmService dmService;

    /** 메시지 전송 (친구에게만) */
    @PostMapping
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        Long toUserId = null;
        Object to = body != null ? body.get("toUserId") : null;
        if (to instanceof Number) toUserId = ((Number) to).longValue();
        else if (to != null) try { toUserId = Long.parseLong(to.toString()); } catch (NumberFormatException ignored) {}
        String text = body != null && body.get("text") != null ? body.get("text").toString() : null;
        try {
            DmMessage msg = dmService.send(userId, toUserId, text);
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("fromUserId", msg.getFromUserId());
            map.put("toUserId", msg.getToUserId());
            map.put("text", msg.getText());
            map.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().format(ISO) : null);
            return ResponseEntity.ok(map);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 특정 친구와의 대화 목록 (최신순) */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam("withUserId") Long withUserId,
            @RequestParam(defaultValue = "50") int limit,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        if (withUserId.equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "자기 자신과의 대화는 없습니다."));
        }
        List<DmMessage> list = dmService.getConversation(userId, withUserId, limit);
        List<Map<String, Object>> items = list.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("fromUserId", m.getFromUserId());
            map.put("toUserId", m.getToUserId());
            map.put("text", m.getText());
            map.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().format(ISO) : null);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("items", items));
    }

    /** 채팅 목록 (대화 상대별 최근 메시지, 최신순) */
    @GetMapping("/conversations")
    public ResponseEntity<?> conversationList(
            @RequestParam(defaultValue = "20") int limit,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        int size = Math.min(Math.max(1, limit), 50);
        List<Map<String, Object>> list = dmService.getConversationList(userId, size);
        return ResponseEntity.ok(Map.of("list", list));
    }
}
