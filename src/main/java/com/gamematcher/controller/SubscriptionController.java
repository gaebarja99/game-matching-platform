package com.gamematcher.controller;

import com.gamematcher.service.PaymentService;
import com.gamematcher.service.SubscriptionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final String SESSION_USER_ID = "userId";

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;

    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam Long userId, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        boolean subscribed = subscriptionService.isSubscribed(myId, userId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("subscribed", subscribed);
        response.put("expiresAt", subscriptionService.getSubscriptionExpiresAt(myId, userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Long> body, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Long streamerId = body != null ? body.get("userId") : null;
        if (streamerId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "스트리머 ID가 필요합니다."));
        }
        if (myId.equals(streamerId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "본인 채널은 구독할 수 없습니다."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "구독은 결제 또는 마일리지 구매를 이용해 주세요."));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createSubscriptionOrder(@RequestBody Map<String, Long> body, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Long streamerId = body != null ? body.get("userId") : null;
        if (streamerId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "스트리머 ID가 필요합니다."));
        }
        try {
            PaymentService.CreateOrderResult result = paymentService.createSubscriptionOrder(myId, streamerId);
            return ResponseEntity.ok(Map.of(
                    "orderId", result.orderId(),
                    "amount", result.amountWon(),
                    "orderName", result.orderName(),
                    "storeId", result.storeId() != null ? result.storeId() : "",
                    "pg", result.pg() != null ? result.pg() : "html5_inicis.INIpayTest",
                    "payMethod", result.payMethod() != null ? result.payMethod() : "card"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmSubscription(@RequestBody Map<String, String> body, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        String orderId = body != null ? body.get("orderId") : null;
        String impUid = body != null ? body.get("impUid") : null;
        if (orderId == null || orderId.isBlank() || impUid == null || impUid.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "orderId와 impUid가 필요합니다."));
        }
        try {
            PaymentService.ConfirmResult result = paymentService.confirmSubscriptionPayment(myId, orderId, impUid);
            return ResponseEntity.ok(Map.of("subscribed", true, "message", result.message()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("subscribed", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("subscribed", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unsubscribe(@PathVariable Long userId, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            subscriptionService.unsubscribe(myId, userId);
            return ResponseEntity.ok(Map.of("subscribed", false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(405).body(Map.of("subscribed", true, "message", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> mySubscriptions(HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "濡쒓렇?몄씠 ?꾩슂?⑸땲??"));
        }
        List<?> list = subscriptionService.getMySubscriptionList(myId).stream()
                .map(s -> Map.of(
                        "userId", s.getUserId(),
                        "nickname", s.getNickname() != null ? s.getNickname() : "",
                        "loginId", s.getLoginId() != null ? s.getLoginId() : "",
                        "profileImageUrl", s.getProfileImageUrl() != null ? s.getProfileImageUrl() : "",
                        "subscribedAt", s.getSubscribedAt() != null ? s.getSubscribedAt().toString() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("list", list, "total", list.size()));
    }

    @GetMapping("/subscribers")
    public ResponseEntity<?> subscribers(HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<?> list = subscriptionService.getSubscriberList(myId).stream()
                .map(s -> Map.of(
                        "userId", s.getUserId(),
                        "nickname", s.getNickname() != null ? s.getNickname() : "",
                        "loginId", s.getLoginId() != null ? s.getLoginId() : "",
                        "profileImageUrl", s.getProfileImageUrl() != null ? s.getProfileImageUrl() : "",
                        "subscribedAt", s.getSubscribedAt() != null ? s.getSubscribedAt().toString() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("list", list, "total", list.size()));
    }
}
