package com.gamematcher.controller;

import com.gamematcher.service.PushTokenService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushTokenController {

    private static final String SESSION_USER_ID = "userId";

    private final PushTokenService pushTokenService;

    @PostMapping("/token")
    public ResponseEntity<?> registerToken(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).build();

        String token = body != null && body.get("token") != null ? body.get("token").toString() : null;
        String platform = body != null && body.get("platform") != null ? body.get("platform").toString() : "web";

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "token is required"));
        }

        pushTokenService.registerToken(userId, token, platform);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/token")
    public ResponseEntity<?> unregisterToken(@RequestParam("token") String token, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).build();
        pushTokenService.unregisterToken(userId, token);
        return ResponseEntity.ok(Map.of("success", true));
    }
}

