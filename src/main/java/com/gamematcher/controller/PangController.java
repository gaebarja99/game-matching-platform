package com.gamematcher.controller;

import com.gamematcher.constant.PangConstants;
import com.gamematcher.dto.pang.ChargeRequest;
import com.gamematcher.entity.PangCharge;
import com.gamematcher.service.PangService;
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
@RequestMapping("/api/pang")
@RequiredArgsConstructor
public class PangController {

    private static final String SESSION_USER_ID = "userId";
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PangService pangService;

    @GetMapping("/balance")
    public ResponseEntity<?> balance(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        long balance = pangService.getBalance(userId);
        return ResponseEntity.ok(java.util.Map.of("balance", balance, "pricePerPang", PangConstants.PRICE_WON_PER_PANG));
    }

    @PostMapping("/charge")
    public ResponseEntity<?> charge(@RequestBody ChargeRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if (request.getAmount() == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "충전할 팡 개수를 입력해 주세요."));
        }
        try {
            long newBalance = pangService.charge(userId, request.getAmount());
            return ResponseEntity.ok(java.util.Map.of("balance", newBalance, "message", "팡이 충전되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /** 내 충전 내역 (최신순) */
    @GetMapping("/charges")
    public ResponseEntity<?> myCharges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<PangCharge> list = pangService.getChargeHistory(userId, page, size);
        long total = pangService.getChargeHistoryCount(userId);
        List<Map<String, Object>> items = list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("pangAmount", c.getPangAmount());
            m.put("priceWon", c.getPriceWon());
            m.put("orderId", c.getOrderId());
            m.put("impUid", c.getImpUid());
            boolean refunded = c.getPangAmount() != null && c.getPangAmount() > 0
                    && pangService.isRefundedCharge(userId, c.getOrderId(), c.getImpUid());
            m.put("refunded", refunded);
            m.put("refundable", !refunded && c.getPangAmount() != null && c.getPangAmount() > 0
                    && ((c.getOrderId() != null && !c.getOrderId().isBlank())
                    || (c.getImpUid() != null && !c.getImpUid().isBlank())));
            m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().format(ISO_FORMAT) : null);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("items", items, "total", total));
    }
}
