package com.gamematcher.controller;

import com.gamematcher.service.FollowService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private static final String SESSION_USER_ID = "userId";

    private final FollowService followService;

    /** 내 채널을 팔로우한 사용자 목록 (스트리머용 팔로워 목록) */
    @GetMapping("/followers")
    public ResponseEntity<?> followers(HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<?> list = followService.getFollowerList(myId).stream()
                .map(f -> Map.of(
                        "userId", f.getUserId(),
                        "nickname", f.getNickname() != null ? f.getNickname() : "",
                        "loginId", f.getLoginId() != null ? f.getLoginId() : "",
                        "followedAt", f.getFollowedAt() != null ? f.getFollowedAt().toString() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("list", list, "total", list.size()));
    }

    /** 내가 팔로우한 채널(사용자) 목록 */
    @GetMapping("/list")
    public ResponseEntity<?> list(HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<?> list = followService.getFollowingList(myId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 현재 로그인 사용자가 대상 사용자를 팔로우 중인지 조회 */
    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam Long userId, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        boolean following = followService.isFollowing(myId, userId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    /** 팔로우 */
    @PostMapping
    public ResponseEntity<?> follow(@RequestBody Map<String, Long> body, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Long targetId = body != null ? body.get("userId") : null;
        if (targetId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID가 필요합니다."));
        }
        followService.follow(myId, targetId);
        return ResponseEntity.ok(Map.of("following", true));
    }

    /** 언팔로우 */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unfollow(@PathVariable Long userId, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        followService.unfollow(myId, userId);
        return ResponseEntity.ok(Map.of("following", false));
    }
}
