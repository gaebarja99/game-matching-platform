package com.gamematcher.controller;

import com.gamematcher.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/follow")
    public ResponseEntity<List<Map<String, Object>>> followRank() {
        return ResponseEntity.ok(rankService.getFollowRank());
    }

    @GetMapping("/pang")
    public ResponseEntity<List<Map<String, Object>>> pangRank() {
        return ResponseEntity.ok(rankService.getPangRank());
    }

    @GetMapping("/viewers")
    public ResponseEntity<List<Map<String, Object>>> viewerRank() {
        return ResponseEntity.ok(rankService.getViewerRank());
    }
}
