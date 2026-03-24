package com.gamematcher.controller;

import com.gamematcher.service.RevenueService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private static final String SESSION_USER_ID = "userId";

    private final RevenueService revenueService;

    /** 내 수익 요약 (받은 팡, 환전 가능, 수수료율) */
    @GetMapping("/me")
    public ResponseEntity<?> getMyRevenue(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(revenueService.getMyRevenue(userId));
    }

    /** 환전 신청 */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Object amountObj = body.get("amount");
        if (amountObj == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "환전할 팡 개수를 입력해 주세요."));
        }
        long amount = 0;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).longValue();
        } else if (amountObj instanceof String) {
            try {
                amount = Long.parseLong((String) amountObj);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "팡 개수는 숫자로 입력해 주세요."));
            }
        }
        try {
            return ResponseEntity.ok(revenueService.withdraw(userId, amount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
