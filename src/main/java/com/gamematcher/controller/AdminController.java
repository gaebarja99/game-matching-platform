package com.gamematcher.controller;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.StreamerTier;
import com.gamematcher.entity.User;
import com.gamematcher.repository.DonationRepository;
import com.gamematcher.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 운영자(ADMIN) 전용 API */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String SESSION_USER_ID = "userId";

    private final UserRepository userRepository;
    private final DonationRepository donationRepository;

    private boolean isAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(u -> u.getRole() == Role.ADMIN)
                .orElse(false);
    }

    /** 스트리머 목록 (id, loginId, displayName, streamerTier, totalReceivedPang) */
    @GetMapping("/streamers")
    public ResponseEntity<?> listStreamers(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> list = users.stream()
                .map(u -> {
                    long totalReceived = donationRepository.sumAmountByToUserId(u.getId());
                    String displayName = u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername();
                    return Map.<String, Object>of(
                            "id", u.getId(),
                            "loginId", u.getLoginId() != null ? u.getLoginId() : "",
                            "displayName", displayName,
                            "streamerTier", u.getStreamerTier() != null ? u.getStreamerTier().name() : "GENERAL",
                            "totalReceivedPang", totalReceived
                    );
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** 스트리머 구분 변경 (GENERAL / PARTNER) */
    @PatchMapping("/streamers/{userId}/tier")
    public ResponseEntity<?> updateStreamerTier(@PathVariable Long userId,
                                               @RequestBody Map<String, String> body,
                                               HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }
        String tierStr = body.get("tier");
        if (tierStr == null || tierStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "등급을 입력해 주세요. (일반 또는 파트너)"));
        }
        StreamerTier tier;
        try {
            tier = StreamerTier.valueOf(tierStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "등급은 일반 또는 파트너만 가능합니다."));
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "사용자를 찾을 수 없습니다."));
        }
        user.setStreamerTier(tier);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "변경되었습니다.", "tier", tier.name()));
    }
}
