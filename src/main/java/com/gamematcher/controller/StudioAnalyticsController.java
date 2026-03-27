package com.gamematcher.controller;

import com.gamematcher.service.StudioAnalyticsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/studio/analytics")
@RequiredArgsConstructor
public class StudioAnalyticsController {

    private static final String SESSION_USER_ID = "userId";

    private final StudioAnalyticsService studioAnalyticsService;

    @GetMapping("/live")
    public ResponseEntity<?> getLiveAnalysis(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (!(userId instanceof Long uid)) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        return ResponseEntity.ok(studioAnalyticsService.getLiveAnalysis(uid));
    }
}
