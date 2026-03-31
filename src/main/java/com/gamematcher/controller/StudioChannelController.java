package com.gamematcher.controller;

import com.gamematcher.service.ChannelPermissionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/studio/channel")
@RequiredArgsConstructor
public class StudioChannelController {

    private static final String SESSION_USER_ID = "userId";

    private final ChannelPermissionService channelPermissionService;

    @GetMapping("/context")
    public ResponseEntity<?> context(@RequestParam(required = false) Long ownerUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(channelPermissionService.getContext(userId, ownerUserId));
    }

    @GetMapping("/permissions")
    public ResponseEntity<?> permissions(@RequestParam(required = false) Long ownerUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Map<String, Object> context = channelPermissionService.getContext(userId, ownerUserId);
        Long targetOwnerUserId = ((Number) context.get("ownerUserId")).longValue();
        return ResponseEntity.ok(Map.of(
                "context", context,
                "list", channelPermissionService.getPermissionList(targetOwnerUserId)
        ));
    }

    @PostMapping("/permissions")
    public ResponseEntity<?> addPermission(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        channelPermissionService.addPermission(
                userId,
                body.get("keyword") != null ? body.get("keyword").toString() : "",
                body.get("role") != null ? body.get("role").toString() : "채널 관리자"
        );
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/permissions/{managerUserId}")
    public ResponseEntity<?> removePermission(@PathVariable Long managerUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        channelPermissionService.removePermission(userId, managerUserId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/manage")
    public ResponseEntity<?> manage(@RequestParam(required = false) Long ownerUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(channelPermissionService.getChannelManage(userId, ownerUserId));
    }

    @PutMapping("/manage")
    public ResponseEntity<?> saveManage(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(channelPermissionService.saveChannelManage(userId, body));
    }
}
