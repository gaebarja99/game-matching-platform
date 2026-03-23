package com.gamematcher.controller;

import com.gamematcher.config.SessionCountListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @GetMapping("/online")
    public ResponseEntity<Map<String, Integer>> online() {
        int count = SessionCountListener.getOnlineCount();
        return ResponseEntity.ok(Map.of("online", count));
    }
}
